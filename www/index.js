const exec=require("cordova/exec");


module.exports={
    start:(data,successCallback,errorCallback)=>{
        const obj={
            data:data
        };
        exec(successCallback,errorCallback,"BackgroundService","startService",[obj]);
    },
    stop:(successCallback,errorCallback)=>{
        exec(successCallback,errorCallback,"BackgroundService","stopService",[]);
    },
}
