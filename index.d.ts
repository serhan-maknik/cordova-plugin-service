declare const BackgroundService:BackgroundService;


interface BackgroundService {
    start(
        data:{
            url:string,
            header:object,
            body:object,
        },
        successCallback:()=>void,
        errorCallback:()=>void,
    ):void,

    stop(
        successCallback:()=>void,
        errorCallback:()=>void,
    ):void,
}
