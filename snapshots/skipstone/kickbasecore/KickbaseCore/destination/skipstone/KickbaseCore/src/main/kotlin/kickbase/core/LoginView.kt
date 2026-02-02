package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*

import skip.ui.*
import skip.foundation.*
import skip.model.*

internal class LoginView: View {
    internal var authManager: AuthenticationManager
        get() = _authManager.wrappedValue
        set(newValue) {
            _authManager.wrappedValue = newValue
        }
    internal var _authManager = skip.ui.Environment<AuthenticationManager>()
    private var email: String
        get() = _email.wrappedValue
        set(newValue) {
            _email.wrappedValue = newValue
        }
    private var _email: skip.ui.State<String>
    private var password: String
        get() = _password.wrappedValue
        set(newValue) {
            _password.wrappedValue = newValue
        }
    private var _password: skip.ui.State<String>
    private var showPassword: Boolean
        get() = _showPassword.wrappedValue
        set(newValue) {
            _showPassword.wrappedValue = newValue
        }
    private var _showPassword: skip.ui.State<Boolean>
    internal var horizontalSizeClass: UserInterfaceSizeClass? = null

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Group { ->
                ComposeBuilder { composectx: ComposeContext ->
                    if (horizontalSizeClass == UserInterfaceSizeClass.regular) {
                        // iPad Layout - horizontal centered
                        iPadLayout.Compose(composectx)
                    } else {
                        // iPhone Layout - vertical centered
                        iPhoneLayout.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }.background(Color.gray.opacity(0.1)).Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedemail by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String>, Any>) { mutableStateOf(_email) }
        _email = rememberedemail

        val rememberedpassword by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String>, Any>) { mutableStateOf(_password) }
        _password = rememberedpassword

        val rememberedshowPassword by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showPassword) }
        _showPassword = rememberedshowPassword

        _authManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = AuthenticationManager::class)!!
        this.horizontalSizeClass = EnvironmentValues.shared.horizontalSizeClass

        return super.Evaluate(context, options)
    }

    private val iPadLayout: View
        get() {
            return HStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Spacer().Compose(composectx)

                    VStack(spacing = 40.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Spacer().Compose(composectx)

                            // Logo/Title
                            logoSection.Compose(composectx)

                            // Login Form - smaller width on iPad
                            VStack(spacing = 20.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    emailField.Compose(composectx)
                                    passwordField.Compose(composectx)
                                    loginButton.Compose(composectx)
                                    demoButton.Compose(composectx)

                                    if (authManager.isLoading) {
                                        ProgressView(LocalizedStringKey(stringLiteral = "Anmeldung läuft..."))
                                            .tint(Color.green).Compose(composectx)
                                    }

                                    authManager.errorMessage?.let { error ->
                                        Text(error)
                                            .foregroundColor(Color.red)
                                            .font(Font.caption)
                                            .multilineTextAlignment(TextAlignment.center).Compose(composectx)
                                    }
                                    ComposeResult.ok
                                }
                            }
                            .frame(maxWidth = 400.0).Compose(composectx) // Begrenzte Breite auf iPad

                            Spacer().Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.horizontal, 40.0).Compose(composectx)

                    Spacer().Compose(composectx)
                    ComposeResult.ok
                }
            }
        }

    private val iPhoneLayout: View
        get() {
            return VStack(spacing = 30.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Spacer().Compose(composectx)

                    // Logo/Title
                    logoSection.Compose(composectx)

                    // Login Form
                    VStack(spacing = 20.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            emailField.Compose(composectx)
                            passwordField.Compose(composectx)
                            loginButton.Compose(composectx)
                            demoButton.Compose(composectx)

                            if (authManager.isLoading) {
                                ProgressView(LocalizedStringKey(stringLiteral = "Anmeldung läuft..."))
                                    .tint(Color.green).Compose(composectx)
                            }

                            authManager.errorMessage?.let { error ->
                                Text(error)
                                    .foregroundColor(Color.red)
                                    .font(Font.caption)
                                    .multilineTextAlignment(TextAlignment.center).Compose(composectx)
                            }
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.horizontal, 30.0).Compose(composectx)

                    Spacer().Compose(composectx)
                    ComposeResult.ok
                }
            }
        }

    private val logoSection: View
        get() {
            return VStack(spacing = 10.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image("AppIconImage")
                        .resizable()
                        .aspectRatio(contentMode = ContentMode.fit)
                        .frame(width = if (horizontalSizeClass == UserInterfaceSizeClass.regular) 160.0 else 120.0, height = if (horizontalSizeClass == UserInterfaceSizeClass.regular) 160.0 else 120.0).Compose(composectx)

                    Text(LocalizedStringKey(stringLiteral = "Kickbase Helper"))
                        .font(if (horizontalSizeClass == UserInterfaceSizeClass.regular) Font.largeTitle else Font.title)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    Text(LocalizedStringKey(stringLiteral = "Verwalten Sie Ihr Team professionell"))
                        .font(Font.subheadline)
                        .foregroundColor(Color.secondary)
                        .multilineTextAlignment(TextAlignment.center).Compose(composectx)
                    ComposeResult.ok
                }
            }
        }

    private val emailField: View
        get() {
            return VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "E-Mail"))
                        .font(Font.headline)
                        .foregroundColor(Color.primary).Compose(composectx)

                    TextField(LocalizedStringKey(stringLiteral = "ihre@email.com"), text = Binding({ _email.wrappedValue }, { it -> _email.wrappedValue = it }))
                        .textFieldStyle(TextFieldStyle.roundedBorder).Compose(composectx)
                    ComposeResult.ok
                }
            }
        }

    private val passwordField: View
        get() {
            return VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Passwort"))
                        .font(Font.headline)
                        .foregroundColor(Color.primary).Compose(composectx)

                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            if (showPassword) {
                                TextField(LocalizedStringKey(stringLiteral = "Passwort"), text = Binding({ _password.wrappedValue }, { it -> _password.wrappedValue = it })).Compose(composectx)
                            } else {
                                SecureField(LocalizedStringKey(stringLiteral = "Passwort"), text = Binding({ _password.wrappedValue }, { it -> _password.wrappedValue = it })).Compose(composectx)
                            }

                            Button(action = { -> showPassword = !showPassword }) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Image(systemName = if (showPassword) "eye.slash" else "eye")
                                        .foregroundColor(Color.gray).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .textFieldStyle(TextFieldStyle.roundedBorder).Compose(composectx)
                    ComposeResult.ok
                }
            }
        }

    private val loginButton: View
        get() {
            return Button(action = { ->
                Task { -> authManager.login(email = email, password = password) }
            }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Anmelden"))
                        .font(Font.headline)
                        .foregroundColor(Color.white)
                        .frame(maxWidth = Double.infinity)
                        .frame(height = 50.0)
                        .background(if (isLoginDisabled) Color.gray else Color.green)
                        .cornerRadius(10.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .disabled(isLoginDisabled)
        }

    private val demoButton: View
        get() {
            return Button(action = { ->
                Task { -> authManager.loginWithDemo() }
            }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "📱 Demo ausprobieren"))
                        .font(Font.headline)
                        .foregroundColor(Color.white)
                        .frame(maxWidth = Double.infinity)
                        .frame(height = 50.0)
                        .background(Color.blue)
                        .cornerRadius(10.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .disabled(authManager.isLoading)
        }

    private val isLoginDisabled: Boolean
        get() = email.isEmpty || password.isEmpty || authManager.isLoading

    private constructor(email: String = "", password: String = "", showPassword: Boolean = false, privatep: Nothing? = null) {
        this._email = skip.ui.State(email)
        this._password = skip.ui.State(password)
        this._showPassword = skip.ui.State(showPassword)
    }

    constructor(): this(privatep = null) {
    }
}

// #Preview omitted
