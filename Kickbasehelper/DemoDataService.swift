import Foundation
import KickbaseCore

class DemoDataService {
    static func createDemoLoginResponse() -> LoginResponse {
        let demoUser = User(
            id: "demo_user_id",
            name: "Demo Manager",
            teamName: "FC Demo 2024",
            email: "demo@kickbasehelper.de",
            budget: 15_000_000,
            teamValue: 185_000_000,
            points: 1250,
            placement: 3,
            flags: 0
        )
        
        // Da LoginResponse Properties 'let' sind, müssen wir einen Encoder/Decoder Trick nutzen 
        // oder einen internen Init public machen.
        // Besser: wir fügen einen public init zu LoginResponse in Models.swift hinzu.
        // Für jetzt: simulieren wir JSON decoding da wir die Models nicht ändern können ohne Context switch.
        // Aber warte, ich kann LoginResponse Init hinzufügen. Das ist sauberer.
        
        // Dummy placeholder return for now until I fix Model
        return LoginResponse(tkn: "DEMO_TOKEN_12345", user: demoUser, leagues: [], userId: "demo_user_id")
    }
}
