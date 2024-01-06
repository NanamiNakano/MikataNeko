package mikataneko.models

class HashVerificationFailedException(target: String): RuntimeException("SHA-1 verification failed: $target")

class MicrosoftAuthenticatorException(cause: Throwable? = null): RuntimeException("Unexpected error in microsoft authenticator",cause)
