Olá! Fico feliz que você tenha feito as alterações para adaptar o projeto ao uso do Gradle. O README está ótimo e, para melhorar ainda mais, fiz algumas revisões para enxugar o texto e corrigir pequenos detalhes. Aqui está a versão ajustada:

---

# Cache de Aplicação com Invalidação Distribuída usando Quarkus, Caffeine e Apache Kafka

Este projeto demonstra como implementar um cache local em uma aplicação Quarkus, utilizando o Caffeine como provedor de cache, e como invalidar esse cache em múltiplas instâncias da aplicação usando mensagens via Apache Kafka. O objetivo é manter a consistência dos dados em ambientes distribuídos sem a complexidade de um cache distribuído.

## Sumário

- [Introdução](#introdução)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Configuração do Projeto](#configuração-do-projeto)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Funcionamento](#funcionamento)
- [Executando a Aplicação](#executando-a-aplicação)
- [Testando a Invalidação de Cache](#testando-a-invalidação-de-cache)
- [Considerações Finais](#considerações-finais)
- [Referências](#referências)

## Introdução

Aplicações modernas frequentemente utilizam cache para melhorar o desempenho e reduzir a latência das respostas. Em ambientes distribuídos, manter a consistência do cache entre múltiplas instâncias pode ser desafiador. Este projeto apresenta uma solução que combina cache local com invalidação distribuída, mantendo a eficiência do cache local e garantindo a consistência dos dados.

## Arquitetura

- **Quarkus**: Framework Java nativo em nuvem para criar aplicações eficientes.
- **Caffeine Cache**: Biblioteca de cache de alto desempenho para Java.
- **Apache Kafka**: Plataforma de streaming distribuída para troca de mensagens.
- **Quarkus Reactive Messaging**: Permite comunicação reativa entre componentes usando mensagens.

## Pré-requisitos

- **Java 21**
- **Apache Kafka**: Instância em execução (local ou remota)
- **Gradle**: Para construir o projeto
- **Quarkus CLI (opcional)**: Para facilitar o desenvolvimento

## Configuração do Projeto

### Dependências no `build.gradle`

As principais dependências incluem:

```gradle
dependencies {
    implementation 'io.quarkus:quarkus-smallrye-health'
    implementation 'io.quarkus:quarkus-smallrye-metrics'
    implementation 'io.quarkus:quarkus-arc'
    implementation 'io.quarkus:quarkus-smallrye-reactive-messaging'
    implementation 'io.quarkus:quarkus-smallrye-reactive-messaging-kafka'
    implementation 'io.quarkus:quarkus-rest'
    implementation 'io.quarkus:quarkus-cache'
    testImplementation 'io.quarkus:quarkus-junit5'
}
```

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/
│   │   └── br/com/haikal/cache/
│   │       ├── DataService.java
│   │       └── DataEntrypoint.java
│   └── resources/
│       └── application.properties
```

- **DataService.java**: Simula um serviço aplicando o cache.
- **DataEntrypoint.java**: Exposição de endpoints REST para interagir com a aplicação.
- **application.properties**: Configurações da aplicação, incluindo cache e Kafka.

## Funcionamento

### 1. Cache Local com Caffeine

O método `getData(String key)` é anotado com `@CacheResult`, indicando que os resultados devem ser armazenados em cache.

```java
@CacheResult(cacheName = MY_CACHE_DATA)
public String getData(String key) {
    LOGGER.info("Os dados foram obtidos da fonte primária. [key:{}]", key);
    // Lógica para obter os dados da fonte primária
    return "Dados de exemplo para a chave " + key;
}
```

### 2. Invalidação de Cache

Quando os dados são atualizados, o método `updateData(String key)` é chamado, que por sua vez envia uma mensagem de invalidação para outras instâncias via Kafka.

```java
public void updateData(String key) {
    LOGGER.info("Atualização de dados invocada. [key:{}]", key);
    // Lógica para atualizar os dados na fonte primária

    // Envia a chave para invalidar o cache em outras instâncias
    LOGGER.info("Notificação de invalidação de cache enviada para outras instâncias. [key:{}]", key);
    emitter.send(key);
}
```

### 3. Recebimento de Mensagens de Invalidação

O método `onCacheInvalidation(String key)` recebe as mensagens de invalidação e chama `invalidateData(String key)` para invalidar o cache local.

```java
@Incoming("cache-invalidation-in")
protected void onCacheInvalidation(String key) {
    LOGGER.info("Solicitação de invalidação de cache recebida para a chave {}", key);
    invalidateData(key);
}

@CacheInvalidate(cacheName = MY_CACHE_DATA)
protected void invalidateData(String key) {
    LOGGER.info("A invalidação do cache foi invocada. [key:{}]", key);
}
```

## Executando a Aplicação

### 1. Inicie o Apache Kafka

Certifique-se de que o Apache Kafka esteja em execução e acessível pela aplicação. Você pode usar um contêiner Docker para executar o Kafka localmente.

### 2. Configure o `application.properties`

As configurações essenciais incluem:

```properties
# Configurações do canal de saída (envio de mensagens)
mp.messaging.outgoing.cache-invalidation-out.connector=smallrye-kafka
mp.messaging.outgoing.cache-invalidation-out.topic=cache-invalidation
mp.messaging.outgoing.cache-invalidation-out.value.serializer=org.apache.kafka.common.serialization.StringSerializer
mp.messaging.outgoing.cache-invalidation-out.bootstrap.servers=localhost:9092

# Configurações do canal de entrada (recebimento de mensagens)
mp.messaging.incoming.cache-invalidation-in.connector=smallrye-kafka
mp.messaging.incoming.cache-invalidation-in.topic=cache-invalidation
mp.messaging.incoming.cache-invalidation-in.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
mp.messaging.incoming.cache-invalidation-in.auto.offset.reset=latest
mp.messaging.incoming.cache-invalidation-in.broadcast=true
mp.messaging.incoming.cache-invalidation-in.bootstrap.servers=localhost:9092

# Configurações de logging
quarkus.log.level=ERROR
quarkus.log.category."br.com.haikal".level=INFO
quarkus.log.category."io.quarkus.cache.runtime.CacheResultInterceptor".level=DEBUG

# Habilitar estatísticas do cache
quarkus.cache.caffeine."my-cache-data".stats-enabled=true
```

### 3. Compile e Execute a Aplicação

Use o Gradle para compilar e executar a aplicação:

```bash
./gradlew build
./gradlew quarkusDev
```

### 4. Escale a Aplicação (Opcional)

Para simular múltiplas instâncias, você pode iniciar a aplicação em diferentes portas ou máquinas. Certifique-se de que todas as instâncias estão apontando para o mesmo cluster Kafka.

## Testando a Invalidação de Cache

### 1. Obtenha Dados via API REST

Faça uma requisição GET para obter dados:

```bash
curl http://localhost:8080/data/get/myKey
```

**Resposta:**

```
Dados de exemplo para a chave myKey
```

Observe nos logs que os dados foram obtidos da fonte primária e armazenados em cache.

### 2. Obtenha Dados Novamente

Repita a requisição:

```bash
curl http://localhost:8080/data/get/myKey
```

Desta vez, os dados serão recuperados do cache, e a fonte primária não será consultada.

### 3. Atualize os Dados

Faça uma requisição para atualizar os dados:

```bash
curl http://localhost:8080/data/update/myKey
```

Isso simula uma atualização dos dados na fonte primária e envia uma mensagem de invalidação.

### 4. Verifique a Invalidação do Cache

Após a atualização, faça novamente a requisição para obter os dados:

```bash
curl http://localhost:8080/data/get/myKey
```

Os dados serão obtidos da fonte primária novamente, indicando que o cache foi invalidado.

### 5. Verifique as Outras Instâncias

Se houver outras instâncias em execução, elas também terão invalidado seus caches locais para a chave `myKey`.

## Considerações Finais

Este projeto demonstra uma abordagem eficiente para manter a consistência do cache em aplicações distribuídas:

- **Cache Local**: Oferece desempenho superior devido ao rápido acesso à memória local.
- **Invalidação Distribuída**: Garante que todas as instâncias mantenham dados atualizados sem a necessidade de um cache centralizado.
- **Simplicidade**: Evita a complexidade adicional de um cache distribuído completo.

### Possíveis Extensões

- **Manuseio de Exceções**: Implementar tratamento de erros na comunicação com o Kafka.
- **Segurança**: Adicionar autenticação e criptografia nas comunicações.
- **Monitoramento**: Integrar ferramentas de monitoramento para acompanhar o desempenho do cache e da mensageria.

## Referências

- [Quarkus - Cache Guide](https://quarkus.io/guides/cache)
- [Quarkus - Reactive Messaging with Kafka](https://quarkus.io/guides/kafka)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Apache Kafka](https://kafka.apache.org/)

---

**Nota:** Este projeto é apenas para fins educacionais e pode ser adaptado conforme as necessidades específicas da sua aplicação.
