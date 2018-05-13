var global = this;
var console = {};
console.log = print;
console.warn = print;
console.error = print;
console.debug = print;
var persistNumeric = function(mongoDatabase, value, metric, time){
	if(mongoDatabase !== undefined)
	{
		var col = mongoDatabase.getCollection("prueba");		
		col.insertOne({"value": value, "metric": metric, "time": time});
	}	
	return {"status": "OK"}
}
