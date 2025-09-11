import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthenticationManager
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showPassword: Bool = false
    @Environment(\.horizontalSizeClass) var horizontalSizeClass
    
    var body: some View {
        GeometryReader { geometry in
            if horizontalSizeClass == .regular {
                // iPad Layout - horizontal centered
                iPadLayout
            } else {
                // iPhone Layout - vertical centered
                iPhoneLayout
            }
        }
        .background(Color(.systemGroupedBackground))
    }
    
    private var iPadLayout: some View {
        HStack {
            Spacer()
            
            VStack(spacing: 40) {
                Spacer()
                
                // Logo/Title
                logoSection
                
                // Login Form - smaller width on iPad
                VStack(spacing: 20) {
                    emailField
                    passwordField
                    loginButton
                    
                    if authManager.isLoading {
                        ProgressView("Anmeldung läuft...")
                            .progressViewStyle(CircularProgressViewStyle(tint: .green))
                    }
                    
                    if let error = authManager.errorMessage {
                        Text(error)
                            .foregroundColor(.red)
                            .font(.caption)
                            .multilineTextAlignment(.center)
                    }
                }
                .frame(maxWidth: 400) // Begrenzte Breite auf iPad
                
                Spacer()
            }
            .padding(.horizontal, 40)
            
            Spacer()
        }
    }
    
    private var iPhoneLayout: some View {
        VStack(spacing: 30) {
            Spacer()
            
            // Logo/Title
            logoSection
            
            // Login Form
            VStack(spacing: 20) {
                emailField
                passwordField
                loginButton
                
                if authManager.isLoading {
                    ProgressView("Anmeldung läuft...")
                        .progressViewStyle(CircularProgressViewStyle(tint: .green))
                }
                
                if let error = authManager.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(.horizontal, 30)
            
            Spacer()
        }
    }
    
    private var logoSection: some View {
        VStack(spacing: 10) {
            Image(systemName: "soccer.ball")
                .font(.system(size: horizontalSizeClass == .regular ? 80 : 60))
                .foregroundColor(.green)
            
            Text("Kickbase Helper")
                .font(horizontalSizeClass == .regular ? .largeTitle : .title)
                .fontWeight(.bold)
            
            Text("Verwalten Sie Ihr Team professionell")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
    }
    
    private var emailField: some View {
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
    }
    
    private var passwordField: some View {
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
                        .foregroundColor(.gray)
                }
            }
            .textFieldStyle(RoundedBorderTextFieldStyle())
        }
    }
    
    private var loginButton: some View {
        Button(action: {
            Task {
                await authManager.login(email: email, password: password)
            }
        }) {
            Text("Anmelden")
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(isLoginDisabled ? Color.gray : Color.green)
                .cornerRadius(10)
        }
        .disabled(isLoginDisabled)
    }
    
    private var isLoginDisabled: Bool {
        email.isEmpty || password.isEmpty || authManager.isLoading
    }
}

#Preview {
    LoginView()
        .environmentObject(AuthenticationManager())
}
