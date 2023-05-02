declare const BackgroundService:BackgroundService;


interface BackgroundService {
    start(
        data:{
            url:string,
            header:object,
            body:object,
        },
        callback:()=>void,
        fallback:()=>void,
    ):void,

    stop(
        callback:()=>void,
        fallback:()=>void,
    ):void,
}
