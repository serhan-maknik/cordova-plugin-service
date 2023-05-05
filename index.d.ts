declare const BackgroundService:BackgroundService;


interface BackgroundService {
    start(
        options:{
            url:string,
            header?:object,
            body?:object,
            notification:{
                title?:string,
                body?:string,
            },
            toast?:string,
        },
        callback:()=>void,
        fallback:()=>void,
    ):void,

    stop(
        callback:()=>void,
        fallback:()=>void,
    ):void,
}
