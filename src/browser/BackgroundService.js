

module.exports={
    /* So the plugin would not generate an error in browser */
    startService:()=>{
        console.log("BackgroundService does not support the browser platform");
    },
    stopService:()=>{
        console.log("BackgroundService does not support the browser platform");
    },
}

