// Contrato de turmas (porta, exceções e evento): fonte única para o monólito e o servico-turmas
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(platform("org.springframework.modulith:spring-modulith-bom:2.1.1"))
    api("org.springframework.modulith:spring-modulith-events-api")   // @Externalized
}
