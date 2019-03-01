package com.aman.learning.microservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@EnableCircuitBreaker
@EnableDiscoveryClient
@EnableZuulProxy
@SpringBootApplication
@EnableBinding(Source.class)
public class ReservationClientApplication {

	@LoadBalanced
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}

}

@RestController
@RequestMapping("/reservations")
class ReservationApiGatewayRestController {

	private RestTemplate restTemplate;

	@Autowired
	private Source source;

	@Autowired
	public ReservationApiGatewayRestController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public Collection<String> fallback() {
		return new ArrayList<String>();
	}

	@HystrixCommand(fallbackMethod = "fallback")
	@RequestMapping("/names")
	public Collection<String> getAllNames() {
		return this.restTemplate.exchange("http://RESERVATION-SERVICE/reservations", HttpMethod.GET, null,
				new ParameterizedTypeReference<Resources<Reservation>>() {
				}).getBody().getContent().stream().map(Reservation::getReservationName).collect(Collectors.toList());
	}

	@RequestMapping(method = RequestMethod.POST)
	public void writeReservationName(@RequestBody Reservation r) {
		Message<String> msg = MessageBuilder.withPayload(r.getReservationName()).build();
		this.source.output().send(msg);
	}
}

class Reservation {

	private String reservationName;

	public String getReservationName() {
		return reservationName;
	}

	public void setReservationName(String reservationName) {
		this.reservationName = reservationName;
	}

}