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
package org.mdpnp.devices.simulation.ecg;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.mdpnp.devices.DeviceClock;
import org.mdpnp.devices.math.DCT;
import org.mdpnp.devices.simulation.NumberWithGradient;
import org.mdpnp.devices.simulation.NumberWithJitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeff Plourde
 *
 */
public class SimulatedElectroCardioGram {

    private static final Logger log = LoggerFactory.getLogger(SimulatedElectroCardioGram.class);

    private int counti = 0, countii = 0, countiii = 0;

    protected int postIncrCounti() {
        int counti = this.counti;
        this.counti = ++this.counti >= i.length ? 0 : this.counti;
        return counti;
    }

    protected int postIncrCountii() {
        int countii = this.countii;
        this.countii = ++this.countii >= ii.length ? 0 : this.countii;
        return countii;
    }

    protected int postIncrCountiii() {
        int countiii = this.countiii;
        this.countiii = ++this.countiii >= iii.length ? 0 : this.countiii;
        return countiii;
    }

    private final class DataPublisher implements Runnable {
        private final Number[] iValues = new Number[samplesPerUpdate];
        private final Number[] iiValues = new Number[samplesPerUpdate];
        private final Number[] iiiValues = new Number[samplesPerUpdate];

        public DataPublisher() {
        }
        
        @Override
        public void run() {

            for (int i = 0; i < iValues.length; i++) {
                iValues[i] = SimulatedElectroCardioGram.this.i[postIncrCounti()];
            }
            for (int i = 0; i < iiValues.length; i++) {
                iiValues[i] = ii[postIncrCountii()];
            }
            for (int i = 0; i < iiiValues.length; i++) {
                iiiValues[i] = iii[postIncrCountiii()];
            }

            DeviceClock.Reading  t = deviceClock.instant();

            int hr = heartRate.intValue();
            int rr = respiratoryRate.intValue();

            receiveECG(t, iValues, iiValues, iiiValues, hr, rr, frequency);
        }

    }

    protected void receiveECG(DeviceClock.Reading sampleTime, Number[] i, Number[] ii, Number[] iii, int heartRate, int respiratoryRate, int frequency) {

    }

    private final double[] iCoeffs = new double[] { 1754.6228740250176, -1.8702723856853978, -17.30350351479403, 0.9639533281850988,
            3.840507863935154, 7.934373158467186, -6.404393385136917, -11.414941750464283, 19.496520796069138, 3.7409186531276273,
            -18.234562841118052, -4.362215895841856, 8.39314891173553, 14.737666088188181, -7.307634318759682, -16.010281409927607, 9.04731657025821,
            10.28516860754574, -7.238889662498312, -9.986662338548209, 5.690957489663056, 14.343622168399994, -5.856023926443699,
            -15.819019082782653, 4.743292467541179, 13.184812184061139, -0.9208500166471449, -12.34872345395124, -0.9011292611453284,
            13.029552723424235, 0.23691062552283945, -11.755941213958566, -1.5799403952760471, 10.99649767557294, 2.7074943728683527,
            -10.37561191416219, -2.462923868277498, 8.680929330738556, 3.472698980815081, -8.047763091193865, -4.502048056925383, 7.314699912134309,
            4.806351080210499, -5.784497526847276, -4.331699159052244, 4.4904861963308464, 3.0810044011396562, -3.3852625827062526,
            -2.907002399513044, 3.3213004933699524, 2.968310519994681, -3.0172149573442795, -2.4879808479466843, 1.4984755189231453,
            2.37241087331865, -0.9052322107446606, -1.5416308378953123, 0.9129171651418391, 0.4386714188406689, -0.5699285253268002,
            -0.22275606968995934, 0.41519296799481736, 0.20601613011919187, -0.3446616347037671, -0.1779753879488293, 0.50809184742382,
            -0.14336241979591546, -0.29337693981419044, 0.232862823993302, -0.19368090118947648, 0.26151693129220965, -0.27582186232649447,
            0.08156520576577218, 0.515399912773259, -0.3805145389457603, -0.17204883681468905, -0.43806240391295787, 0.42020371010864155,
            0.8235399604387487, -0.48410990866553527, -0.6071657750002843, 0.1113405985006294, 0.5462241396121782, 0.04888487986850741,
            -0.7134747194583869, 0.021055539807231443, 1.0200176082677876, -0.32488506383207666, -0.8663528601192862, 0.33094291236668866,
            0.5021564359516518, 0.061877354509690057, -0.5754442537482416, -0.2604030723137923, 0.7591802843658986, 0.2735780886310309,
            -0.7489485727805001, -0.15583346646506152, 0.5305168689712589, -0.1109387910334687, -0.2137678819653604, 0.20877619408605297,
            0.0513900685492815, 0.1911798101446551, -0.3988288072420691, -0.4530666880286689, 0.808174186519911, -0.036355493780698786,
            -0.34061794120752115, 0.12561989237569826, 0.15481848243225202, 0.07916170020066284, -0.7155265224872169, 0.28848912572964086,
            0.4582217802787396, 0.05686507424434324, -0.3938575012278817, -0.3563576239696431, 0.8722446088367648, -0.39008693750975965,
            -0.3683782246412279, 0.09454303529439813, 0.3763450898215146, 0.4641960755587408, -0.9631379642834486, 0.13681510102782538,
            0.34656803678138254, -0.21096568141076683, 0.10817136833160185, -0.11940537809433986, 0.10550589604313972, 0.2561726146078647,
            -0.1989088439717972, -0.4463585044058828, 0.33249992143634177, 0.015176960832984783, 0.16304595493053586, 0.11340914363202088,
            -0.5691635120953521, 0.4078661818735108, 0.045503850360223176, -0.2519504545676562, -0.16679535657861314, 0.23743472338114296,
            0.6027243234111632, -0.6610841813127941, -0.13417036455209305, 0.2924242884878496, -0.05702357111764156, 0.013239930692747103,
            -0.3189106062838115, 0.5048828652791527, 0.2908350030787716, -0.704178371361135, -0.207381505298466, 0.464299249536071,
            0.308347088394838, -0.5406227178694947, 0.005867268821956306, 0.5084135242256143, -0.15728587973225508, -0.33834345837777996,
            -0.3295196201987764, 0.6600299628857773, 0.38504149252581144, -0.7227385155566506, -0.14312478494621206, 0.38358401238368683,
            0.29956384890816923, -0.4940321912318133, -0.2726959645836895, 0.5086884460085029, 0.28731588032202093, -0.3572576969105404,
            -0.4409266250514879, 0.4249738076261812, 0.24855189537022412, -0.2453669100927395, -0.3398360409407175, 0.26300333632107076,
            0.483245097265392, -0.4033357718930025, -0.2781298095914827, 0.07371141072286519, 0.3453851804331789, -0.031228267124204182,
            -0.25678741665677995, 0.1584739609314728, 0.038595049674687076, -0.05286297126684661, -0.2325566295620568, 0.10557431746476192,
            0.31626014910619904 };
    private final double[] iiCoeffs = new double[] { 1755.4866529259161, 1.1114976697242938, -21.999291501350186, 3.7507486839073074,
            -1.8131820563363779, 11.251136998741035, -3.205596146011037, -16.681573202041967, 25.839291433877786, 2.005658436474591,
            -22.613071041216433, -5.007243441076134, 10.581428489807765, 20.12816133502909, -9.928758931517631, -21.646820067368832,
            12.879011611670665, 12.787396847272962, -11.072140687539134, -11.541833577616403, 8.425692422105051, 20.48521938727036,
            -11.37332723438074, -22.09024782096294, 8.381432105401693, 20.065994837294046, -2.692145416479796, -19.64490621131994,
            0.8420819493809281, 20.131443067282188, -1.5865628911585852, -19.71984681840013, 0.7828556976774133, 18.714623688765563,
            1.7996586382910271, -18.655958762788224, -2.148146729963001, 16.61907735479406, 3.2997959813771574, -14.99422326943353,
            -4.929856727160056, 13.902473053856578, 5.174406335447017, -11.076947655970603, -5.342177692057183, 8.864407966523446, 4.466231146212332,
            -7.340242955165638, -4.321942995266801, 7.612341387997159, 3.1139203279042604, -5.545144204855702, -3.5568914269658825,
            3.4461093566143752, 3.4852526069840173, -2.148414465574199, -1.9518075408390694, 1.3486453312521927, 0.4190399284699819,
            -1.1483912180966698, 0.6556793963545032, 0.3000039643789477, -0.07187684714311468, -0.3404813172001615, 0.2711779522148521,
            -0.17443959953600585, -0.5139035058554168, 0.5618724692772044, 0.8095820715615426, -0.6432760119210491, -1.1748334934479476,
            0.8736279569957486, 0.5423173324803415, 0.12357324734585289, -0.7364537808746163, -0.09066021826811373, -0.04479918196669542,
            0.5251437854928722, -0.05218577688035066, 0.06510678482022636, -0.20489213603412457, -0.3807518530226602, 0.5192958581640492,
            0.0016848180861124128, -0.39564076152529704, -0.054327217696287386, 1.1158064695099814, -0.80878983528635, -0.3771222299442618,
            -0.3492259739270084, 1.5042712486712393, -0.2537405965417426, -0.9758205671699676, -0.3765449703550486, 1.4802655234859514,
            -0.21813202114124153, -0.6898236669141642, -0.22838616210909443, 0.8084370677635698, 0.02867925931836534, -0.8517139323153968,
            0.1848147570025516, 0.7516407398612139, 0.24439500256387406, -1.2225084763779583, 0.04281857173862997, 0.5338621034736952,
            0.4289292410818806, -0.4463689424871312, -0.2113527509167517, 0.14677242084391012, 0.5270707292284442, -0.9415476386411874,
            -0.030611492008460892, 0.9719164887538273, -0.06705606230588577, -0.6299001735230665, -0.039336434567912706, 0.30603705658892066,
            0.0029626332196244676, -0.09841268370638082, 0.01972779901828815, 0.06847011202818903, 0.14187833674522735, -0.3487419456782849,
            0.06862459515873533, 0.23566989529778257, -0.24572700406191697, -0.021954795906832497, 0.5206913375788641, -0.6822309457430191,
            0.24991472064312317, 0.19086623495761865, -0.061557229391546384, -0.3028402591074898, 0.09024596214876394, 0.24806494546799898,
            0.09919652164339227, -0.41624753272765097, 0.11595462884567839, 0.2326762982481459, -0.14414069116061135, -0.2251345250724559,
            0.024125278706905555, 0.7247586399033347, -0.5261851007711975, -0.5082809002759796, 0.5704282711677484, 0.3979120076161909,
            -0.7576417199307376, -0.08554050500379344, 0.6339688745197071, 0.2337981276200672, -0.7607744304904669, -0.39850398659501357,
            1.0457568139965467, 0.19555290277347262, -0.9088533058130258, -0.2917114965366568, 1.0483456380934215, 0.023969092142564558,
            -0.7346789157927114, -0.2819979596619073, 0.9707249375096426, 0.08564310622916678, -0.839630306434462, -0.15970161573828406,
            0.6795033794499732, 0.44120061644822084, -0.7396617520117066, -0.3402012510263651, 0.45768983193352997, 0.5055142606388664,
            -0.7430288554259636, 0.023535136581433677, 0.2552714047318934, 0.5052849007683076, -0.7564914153474973, -0.10607975363112267,
            0.2519897562467573, 0.4174469286338692, -0.3649123884704047, 7.809641218092968E-4, -0.11674158075986803, 0.29424730068092586,
            -0.1836097952302513, -0.10336013198023253, 0.07555684213526162, 0.2299148241397147, -0.10278534578585176, -0.16333516600326944,
            -0.028995223825419515, 0.2105531030934267 };
    private final double[] iiiCoeffs = new double[] { 1744.9053613899102, -2.3697642076067447, -4.894468636077983, 1.8614564805868414,
            -4.196468724764692, 3.5616726828990606, 1.5422435672343275, -3.689243531692336, 6.002892425929955, -0.4460341122404997,
            -6.469194279593086, -0.7103107296677198, 2.7077675490021407, 5.636627433252962, -1.9271042954637103, -5.744871978818023,
            2.5093726972049737, 2.533778125582368, -1.738725889139828, -3.0551035076636452, 1.8584400062409798, 6.19368874584013, -3.013962954759492,
            -7.088782387837067, 2.404938870993566, 6.557527123186383, -0.953034866883336, -6.9165964495693055, 1.0528507905720137, 7.031927638134265,
            -1.094407633262177, -6.432723369914578, -0.445837422751074, 6.617312005954542, 1.2600958843390444, -7.185750847841494,
            -0.336584219611358, 6.884425553705584, 0.09286375588067673, -6.492432027016944, -1.1887310684042816, 6.339446636174751,
            1.7411796074710002, -5.074674169060281, -1.5889598614229692, 3.27264380271494, 1.809773088074508, -3.1142939028845276,
            -1.8726025904817838, 3.9649601435748694, 1.247748874038023, -3.443261179723516, -0.9056357745584673, 1.8201229709154554,
            1.0829062463683243, -0.8834894290090429, -0.9068925559049712, 0.6015541957408352, 0.5568348209455234, -0.21786978876768398,
            -0.6116488197414156, 0.0046177298515237, 0.42386069785997377, -0.060544608725796124, 0.3050301235591947, -0.07304461937799363,
            -0.5308998180365204, 0.2543903222414279, 0.06297534385574771, -0.28546520081720395, -0.0287746392571132, 0.6116483092488247,
            0.5265346305933353, -1.041502395288664, -0.5682998269712963, 0.6683022401294303, 0.1401519362232657, 0.21307696898324294,
            0.023610220853632874, -0.3759756226432289, 0.09350260651607405, -0.16414767088070145, -0.1508271584265469, 0.42743680846037146,
            0.1373097101426389, -0.2826949299798152, 0.025918771634402454, 0.16929485455880822, -0.37039059309546535, -0.019688726440082193,
            0.47272496462580826, -0.2734206272148977, -0.19902641304502017, 0.3907646540619892, 0.10313719433667737, -0.2019402447388208,
            -0.3536905120670117, -0.06013797233974881, 0.4174315365423282, 0.21549508128890424, -0.18988371432560538, -0.10992256566231953,
            0.16398281083962155, -0.31669119107834315, -0.4008356873815614, 0.6454293582748872, 0.47580082930146006, -0.4279644924489879,
            -0.2999409685706979, -0.016567626640311202, 0.159507975800641, 0.06976704841214883, -0.1964035772188546, 0.2226378536545342,
            0.31304538992554115, -0.39118884391505865, -0.35395559837717866, 0.32587442192770494, 0.2706608414260479, -0.24373273589225872,
            -0.21745243265683026, 0.23587429953007366, 0.30532698488281595, -0.2056925819980736, -0.32215849598494, 0.08176024727803496,
            0.07399767711578412, 0.03533591082561288, 0.18523424809591535, 0.023856635236271906, -0.11594188129786809, -0.19885649281860374,
            -0.1404941765189587, 0.21298010969117212, 0.15684654019050778, -0.0023588603937439445, 0.17729660722670826, -0.17932109347087263,
            -0.5116685403267064, 0.1634680982746799, 0.48516580121111047, -0.04178248426655933, -0.21527805745377226, -0.04601371310745799,
            0.13762313039110544, 0.031009929992769764, -0.3251961033493592, 0.07635311287451599, 0.394293557542018, -0.17585264432264006,
            -0.21322772549561647, 0.2136406808563419, 0.10610104253428024, -0.23792660129918572, -0.19710811525882901, 0.20981230096175366,
            0.20772303835309258, -0.07654596606152196, -0.011106317451722898, 0.05239561071318928, -0.19129765752176975, -0.3259501001027798,
            0.23473041220675792, 0.6024180145133339, -0.14189172175815543, -0.5233274327658439, 0.054449756621941264, 0.27232257796438236,
            -0.13660539910356637, -0.21308300385706583, 0.3102151746128963, 0.3378655690249117, -0.2792627034349496, -0.4811077982633953,
            0.0758888168167366, 0.5802102883801812, -0.0697224741040024, -0.54521847853211, 0.22341829993453038, 0.36350203432200623,
            -0.11147058181721038, -0.23996059740224154, -0.2074154247642755, 0.2501118934015001, 0.2473271717338924, -0.22442461488316465,
            -0.007120281258064755, 0.1735296921785799, -0.0606196345607449, -0.23888200290705983, -0.15772112481100498, 0.24694950579610214,
            0.3752874229239964 };
    private final double[] i = new double[iCoeffs.length];
    private final double[] ii = new double[iiCoeffs.length];
    private final double[] iii = new double[iiiCoeffs.length];

    private Number heartRate       = new NumberWithJitter(60, 1, 5);
    private Number respiratoryRate = new NumberWithJitter(12, 1, 2);

    public void setTargetHeartRate(Number targetHeartRate) {
        this.heartRate = new NumberWithGradient(heartRate, targetHeartRate, 5);
        log.debug("Set heartRate to " + this.heartRate);
    }

    public void setTargetRespiratoryRate(Number targetRespiratoryRate) {
        this.respiratoryRate = new NumberWithGradient(respiratoryRate, targetRespiratoryRate, 2);
        log.debug("Set respiratoryRate to " + this.respiratoryRate);
    }

    private void initWaves() {
        DCT.idct(iCoeffs, i);
        DCT.idct(iiCoeffs, ii);
        DCT.idct(iiiCoeffs, iii);
    }

    private ScheduledFuture<?> task;

    public void connect(ScheduledExecutorService executor) {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        long now = System.currentTimeMillis();
        task = executor.scheduleAtFixedRate(new DataPublisher(),
                                            updatePeriod - now % updatePeriod,  // initialDelay
                                            updatePeriod,                       // period
                                            TimeUnit.MILLISECONDS);             // time unit
    }

    public void disconnect() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    public SimulatedElectroCardioGram(DeviceClock referenceClock) {
        this(referenceClock, UPDATE_PERIOD, MS_PER_SAMPLE);
    }

    public SimulatedElectroCardioGram(DeviceClock referenceClock, long updatePeriod, int msPerSample) {
        this(referenceClock, updatePeriod, msPerSample, TS_TYPE, CLOCK_DRIFT_MS);
    }

    public SimulatedElectroCardioGram(final DeviceClock referenceClock,
                                      final long updatePeriod, final int msPerSample,
                                      final TimestampType tsPolicy, final long clockDriftMs) {
        this.updatePeriod = updatePeriod;
        this.msPerSample = msPerSample;
        this.deviceClock = new DeviceClock() {
            final DeviceClock dev=new FuzzyClock(clockDriftMs, tsPolicy);
            @Override
            public Reading instant() {
                return new CombinedReading(referenceClock.instant(), dev.instant());
            }
        };

        this.samplesPerUpdate = (int) Math.floor(updatePeriod / msPerSample);
        this.frequency = (int)(1000.0 / msPerSample);

        initWaves();
    }

    final long updatePeriod;
    final int msPerSample;
    final DeviceClock deviceClock;

    final int samplesPerUpdate;
    final int frequency;

    enum TimestampType {
        realtime,  // real clock
        metronome, // normalized aka 0-15-30-45-0
        drift      // real clock with possible drift
    }

    // defaults
    private static final long UPDATE_PERIOD     = Long.getLong("SimulatedElectroCardioGram.UPDATE_PERIOD", 1000L);
    private static final int MS_PER_SAMPLE      = Integer.getInteger("SimulatedElectroCardioGram.UPDATE_PERIOD", 5);
    private static final TimestampType TS_TYPE  = TimestampType.valueOf(System.getProperty("SimulatedElectroCardioGram.TS_TYPE", "metronome"));
    private static final int CLOCK_DRIFT_MS     = Integer.getInteger("SimulatedElectroCardioGram.CLOCK_DRIFT_MS", 0);

    class FuzzyClock extends DeviceClock.WallClock {

        public FuzzyClock(long clockDriftMs, TimestampType timestampType) {
            this.clockDriftMs = clockDriftMs;
            this.timestampType = timestampType;
        }

        final long clockDriftMs;
        final TimestampType timestampType;

        /**
         *  @return time stamp for the current data sample. few possible choices depending on the timestamp policy:
         *  if 'drift' - returns real clock with possible random drift.
         *  if 'metronome' - normalized aka 0-15-30-45-0 based on the update period
         *  if 'realtime' - always returns wll time, but if will (optionally if clockDrift is not 0) pause the thread for some random drift value
         */

        @Override
        protected long getTimeInMillis() {
            long now;
            switch (timestampType) {
                case drift:
                    long drift = clockDriftMs==0?0L:(long)((clockDriftMs - 2*clockDriftMs*Math.random()));
                    now = System.currentTimeMillis();
                    now = now + drift;
                    break;
                case metronome:
                    now = System.currentTimeMillis();
                    now = now-now%updatePeriod;
                    break;
                case realtime:
                default:
                    long sleep = clockDriftMs==0?0L:(long)(clockDriftMs*Math.random());
                    if(sleep != 0)
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                            // too bad, almost harmless.
                        }
                    now = System.currentTimeMillis();
                    break;
            }
            return now;
        }
    }
}
