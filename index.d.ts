declare const BackgroundService:BackgroundService;


interface BackgroundService {
    start(
        options:{
            url:string,
            header?:object,
            body?:object,
            notification?:{
                /**
                 * @default "BackgroundService is running"
                 */
                title?:string,
                /**
                 * @default ""
                 */
                body?:string,
            },
            toast?:{
                start?:string,
                stop?:string
            },
        },
        callback:()=>void,
        fallback:(error:Error)=>void,
    ):void,

    stop(
        callback:()=>void,
        fallback:(error:Error)=>void,
    ):void,
}
