declare const ServicePlugin:ServicePlugin;


interface ServicePlugin {
    start(
        data:any,
        successCallback:()=>void,
        errorCallback:()=>void,
    ):void,

    stop(
        successCallback:()=>void,
        errorCallback:()=>void,
    ):void,
}
