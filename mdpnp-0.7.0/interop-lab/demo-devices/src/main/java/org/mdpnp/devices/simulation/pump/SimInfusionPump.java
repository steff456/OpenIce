/*******************************************************************************
 * Copyright (c) 2014, MD PnP Program
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.mdpnp.devices.simulation.pump;

import ice.InfusionObjective;
import ice.InfusionStatusDataWriter;

import org.mdpnp.devices.simulation.AbstractSimulatedConnectedDevice;
import org.mdpnp.rtiapi.data.EventLoop;
import org.mdpnp.rtiapi.data.EventLoop.ConditionHandler;
import org.mdpnp.rtiapi.data.QosProfiles;
import org.mdpnp.rtiapi.data.TopicUtil;

import com.rti.dds.infrastructure.Condition;
import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.infrastructure.StringSeq;
import com.rti.dds.publication.Publisher;
import com.rti.dds.subscription.InstanceStateKind;
import com.rti.dds.subscription.QueryCondition;
import com.rti.dds.subscription.ReadCondition;
import com.rti.dds.subscription.SampleInfo;
import com.rti.dds.subscription.SampleInfoSeq;
import com.rti.dds.subscription.SampleStateKind;
import com.rti.dds.subscription.Subscriber;
import com.rti.dds.subscription.ViewStateKind;
import com.rti.dds.topic.Topic;

/**
 * @author Jeff Plourde
 *
 */
public class SimInfusionPump extends AbstractSimulatedConnectedDevice {

    private ice.InfusionStatus infusionStatus = (ice.InfusionStatus) ice.InfusionStatus.create();
    private InstanceHandle_t infusionStatusHandle;

    private ice.InfusionStatusDataWriter infusionStatusWriter;
    private ice.InfusionObjectiveDataReader infusionObjectiveReader;
    private QueryCondition infusionObjectiveQueryCondition;
    private Topic infusionStatusTopic, infusionObjectiveTopic;

    private class MySimulatedInfusionPump extends SimulatedInfusionPump {
        @Override
        protected void receivePumpStatus(String drugName, boolean infusionActive, int drugMassMcg, int solutionVolumeMl, int volumeToBeInfusedMl,
                int infusionDurationSeconds, float infusionFractionComplete) {
            infusionStatus.drug_name = drugName;
            infusionStatus.infusionActive = infusionActive;
            infusionStatus.drug_mass_mcg = drugMassMcg;
            infusionStatus.solution_volume_ml = solutionVolumeMl;
            infusionStatus.volume_to_be_infused_ml = volumeToBeInfusedMl;
            infusionStatus.infusion_duration_seconds = infusionDurationSeconds;
            infusionStatus.infusion_fraction_complete = infusionFractionComplete;
            infusionStatusWriter.write(infusionStatus, infusionStatusHandle);
        }
    }

    @Override
    public boolean connect(String str) {
        pump.connect(executor);
        return super.connect(str);
    }

    @Override
    public void disconnect() {
        pump.disconnect();
        super.disconnect();
    }

    private final MySimulatedInfusionPump pump = new MySimulatedInfusionPump();

    protected void writeIdentity() {
        deviceIdentity.model = "Infusion Pump (Simulated)";
        writeDeviceIdentity();
    }

    protected void stopThePump(boolean stopThePump) {
        pump.setInterlockStop(stopThePump);
    }

    public SimInfusionPump(final Subscriber subscriber, final Publisher publisher, EventLoop eventLoop) {
        super(subscriber, publisher, eventLoop);

        writeIdentity();

        ice.InfusionStatusTypeSupport.register_type(getParticipant(), ice.InfusionStatusTypeSupport.get_type_name());
        infusionStatusTopic = TopicUtil.findOrCreateTopic(domainParticipant, ice.InfusionStatusTopic.VALUE, ice.InfusionStatusTypeSupport.class);
        infusionStatusWriter = (InfusionStatusDataWriter) publisher.create_datawriter_with_profile(infusionStatusTopic, QosProfiles.ice_library,
                QosProfiles.state, null, StatusKind.STATUS_MASK_NONE);

        infusionStatus.unique_device_identifier = deviceIdentity.unique_device_identifier;
        infusionStatusHandle = infusionStatusWriter.register_instance(infusionStatus);

        infusionStatus.drug_name = "Morphine";
        infusionStatus.drug_mass_mcg = 20;
        infusionStatus.solution_volume_ml = 120;
        infusionStatus.infusion_duration_seconds = 3600;
        infusionStatus.infusion_fraction_complete = 0f;
        infusionStatus.infusionActive = true;
        infusionStatus.volume_to_be_infused_ml = 100;

        infusionStatusWriter.write(infusionStatus, infusionStatusHandle);

        ice.InfusionObjectiveTypeSupport.register_type(getParticipant(), ice.InfusionObjectiveTypeSupport.get_type_name());
        infusionObjectiveTopic = TopicUtil.findOrCreateTopic(getParticipant(), ice.InfusionObjectiveTopic.VALUE, ice.InfusionObjectiveTypeSupport.class);

        infusionObjectiveReader = (ice.InfusionObjectiveDataReader) subscriber.create_datareader_with_profile(infusionObjectiveTopic,
                QosProfiles.ice_library, QosProfiles.state, null, StatusKind.STATUS_MASK_NONE);

        StringSeq params = new StringSeq();
        params.add("'" + deviceIdentity.unique_device_identifier + "'");
        infusionObjectiveQueryCondition = infusionObjectiveReader.create_querycondition(SampleStateKind.NOT_READ_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE, "unique_device_identifier = %0", params);
        eventLoop.addHandler(infusionObjectiveQueryCondition, new ConditionHandler() {
            private ice.InfusionObjectiveSeq data_seq = new ice.InfusionObjectiveSeq();
            private SampleInfoSeq info_seq = new SampleInfoSeq();

            @Override
            public void conditionChanged(Condition condition) {

                for (;;) {
                    try {
                        infusionObjectiveReader.read_w_condition(data_seq, info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                                (ReadCondition) condition);
                        for (int i = 0; i < info_seq.size(); i++) {
                            SampleInfo si = (SampleInfo) info_seq.get(i);
                            ice.InfusionObjective data = (InfusionObjective) data_seq.get(i);
                            if (si.valid_data) {
                                stopThePump(data.stopInfusion);
                            }
                        }
                    } catch (RETCODE_NO_DATA noData) {
                        break;
                    } finally {
                        infusionObjectiveReader.return_loan(data_seq, info_seq);
                    }
                }
            }

        });

    }

    @Override
    public void shutdown() {
        eventLoop.removeHandler(infusionObjectiveQueryCondition);
        infusionObjectiveReader.delete_readcondition(infusionObjectiveQueryCondition);
        infusionObjectiveQueryCondition = null;

        subscriber.delete_datareader(infusionObjectiveReader);
        infusionObjectiveReader = null;

        getParticipant().delete_topic(infusionObjectiveTopic);
        infusionObjectiveTopic = null;

        infusionStatusWriter.unregister_instance(infusionStatus, infusionStatusHandle);
        infusionStatusHandle = null;

        publisher.delete_datawriter(infusionStatusWriter);
        infusionStatusWriter = null;

        getParticipant().delete_topic(infusionStatusTopic);
        infusionStatusTopic = null;

        super.shutdown();
    }

    @Override
    protected String iconResourceName() {
        return "pump.png";
    }

}
