const exec=require("cordova/exec");


module.exports={
    start:(data,successCallback,errorCallback)=>{
        const obj={
            data:data
        };
        exec(successCallback,errorCallback,"ServicePlugin","startService",[obj]);
    },
    stop:(successCallback,errorCallback)=>{
        exec(successCallback,errorCallback,"ServicePlugin","stopService",[]);
    },
}
