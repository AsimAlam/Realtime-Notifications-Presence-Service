package org.example.realtimenotify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Value("${app.websocket.use-broker-relay:false}")
  private boolean useBrokerRelay;

  @Value("${app.websocket.relay-host:rabbitmq}")
  private String relayHost;

  @Value("${app.websocket.relay-port:61613}")
  private int relayPort;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .setHandshakeHandler(new UserHandshakeHandler())
        .addInterceptors(new JwtHandshakeInterceptor())
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    if (useBrokerRelay) {
      registry
          .enableStompBrokerRelay("/topic", "/queue")
          .setRelayHost(relayHost)
          .setRelayPort(relayPort);
    } else {
      registry.enableSimpleBroker("/topic", "/queue");
    }
    registry.setApplicationDestinationPrefixes("/app");
  }
}
