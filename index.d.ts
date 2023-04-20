declare const BackgroundService:BackgroundService;


interface BackgroundService {
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
