import Foundation;


class ServicePlugin:ServicePluginPlugin {

    @objc(coolAlert:)
    func coolAlert(command:CDVInvokedUrlCommand){
        let message=command.arguments[0] as? String;
        success(command,message);
    }

}