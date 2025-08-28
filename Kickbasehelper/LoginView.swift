import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthenticationManager
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showPassword: Bool = false
    
    var body: some View {
        VStack(spacing: 30) {
            // Logo/Title
            VStack(spacing: 10) {
                Image(systemName: "soccer.ball")
                    .font(.system(size: 60))
                    .foregroundColor(.green)
                
                Text("Kickbase Helper")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Text("Verwalten Sie Ihr Team professionell")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            // Login Form
            VStack(spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("E-Mail")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    TextField("ihre@email.com", text: $email)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                }
                
                VStack(alignment: .leading, spacing: 8) {
                    Text("Passwort")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    HStack {
                        if showPassword {
                            TextField("Passwort", text: $password)
                        } else {
                            SecureField("Passwort", text: $password)
                        }
                        
                        Button(action: {
                            showPassword.toggle()
                        }) {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundColor(.secondary)
                        }
                    }
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                
                Button(action: {
                    Task {
                        await authManager.login(email: email, password: password)
                    }
                }) {
                    HStack {
                        if authManager.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                        }
                        
                        Text("Anmelden")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(authManager.isLoading || email.isEmpty || password.isEmpty)
                
                if let errorMessage = authManager.errorMessage {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }
            }
            
            Spacer()
            
            Text("Verwenden Sie Ihre Kickbase-Zugangsdaten")
                .font(.footnote)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 30)
        .padding(.vertical, 50)
    }
}

#Preview {
    LoginView()
        .environmentObject(AuthenticationManager())
}