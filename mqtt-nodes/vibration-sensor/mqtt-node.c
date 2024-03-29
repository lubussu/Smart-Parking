#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "os/sys/log.h"
#include "mqtt-node.h" // .h all'interno della stessa directory del .c
#include <string.h>
#include <strings.h>
#include "os/dev/leds.h"
/*---------------------------------------------------------------------------*/

#define LOG_MODULE "mqtt-client"
#ifdef MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif


/*---------------------------------------------------------------------------*/
/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Defaukt config values
#define DEFAULT_BROKER_PORT         1883
//#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)


// We assume that the broker does not require authentication


/*---------------------------------------------------------------------------*/
/* Various states */
static uint8_t state;

#define STATE_INIT    		  0
#define STATE_NET_OK    	  1
#define STATE_CONNECTING      2
#define STATE_CONNECTED       3
#define STATE_SUBSCRIBED      4
#define STATE_DISCONNECTED    5

PROCESS(mqtt_client_example, "mqtt_client_example");
AUTOSTART_PROCESSES(&mqtt_client_example);

// Maximum TCP segment size for outgoing segments of our socket
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN   64

// Buffers for Client ID and Topics. Make sure they are large enough to hold the entire respective string
#define BUFFER_SIZE 64

#define VIBRATION_LOWER_BOUND 15
#define VIBRATION_STD 10
static int vibration = 0;

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];

// Periodic timer to check the state of the MQTT client
//#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)

#define PUBLISHING_INTERVAL (5 * CLOCK_SECOND)
//static struct etimer periodic_timer;

// The main MQTT buffers. We will need to increase if we start publishing more data.
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

// pointer to connection
static struct mqtt_connection conn;

//connection status
mqtt_status_t status;

static char broker_address[CONFIG_IP_ADDR_STR_LEN];


// check that the network is operational
static bool have_connectivity(void) {
	if(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL) {
		return false;
	}
	return true;
}

static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data){

	printf("Event occurs\n");
	switch(event) {
		case MQTT_EVENT_CONNECTED: {
			printf("Application has a MQTT connection\n");
			leds_on(1);
			state = STATE_CONNECTED;
			break;
		}
		case MQTT_EVENT_DISCONNECTED: {
			printf("MQTT Disconnect. Reason %u\n", *((mqtt_event_t *)data));
			leds_off(15);
			state = STATE_DISCONNECTED;
			process_poll(&mqtt_client_example);
			break;
		}
		case MQTT_EVENT_PUBLISH: {
			printf("Publishing..");
			break;
		}
		case MQTT_EVENT_PUBACK: {
			printf("Publishing complete.\n");
			break;
		}
		default:
			printf("Application got a unhandled MQTT event: %i\n", event);
			break;
	}
}

static button_hal_button_t *btn;   //Pointer to the button

static void sensor_sample(){
	if(state==STATE_INIT){
		if(have_connectivity()==true)  
			state = STATE_NET_OK;
	} 

	if(state == STATE_NET_OK){
		// Connect to MQTT server
		printf("Connecting!\n");

		memcpy(broker_address, broker_ip, strlen(broker_ip));

		mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
			PUBLISHING_INTERVAL, MQTT_CLEAN_SESSION_ON);
		state = STATE_CONNECTING;
	}
			
					
	if(state == STATE_CONNECTED){
		// Publish something
		sprintf(pub_topic, "%s", "vibration");
				
		// sense the vibration
		if(rand() % 4 == 0){
			vibration = rand() % 6;
		}else{
			vibration = VIBRATION_LOWER_BOUND + rand() % VIBRATION_STD;
		}

		//AGGIUNGERE parking_id
		sprintf(app_buffer, "{\"app\":\"smart_parking\",\n\"value\":%d,\n\"parking_id\":1,\n\"MAC\": \"%s\"}", vibration, client_id);
		printf("Publishing: %s\n", app_buffer);
					
		mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

	} else if ( state == STATE_DISCONNECTED ){
		LOG_ERR("Disconnected form MQTT broker. Trying to reconnect..\n");	
		// Recover from error
		state = STATE_INIT;
	}
}


PROCESS_THREAD(mqtt_client_example, ev, data){

	PROCESS_BEGIN();

	printf("MQTT Client Process\n");

	// Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
						linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
						linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
						linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

	printf("MAC ADDRESS: %s\n", client_id);

	// Broker registration					 
	mqtt_register(&conn, &mqtt_client_example, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
					
	state = STATE_INIT;
					
	// Initialize periodic timer to check the status 
	//etimer_set(&periodic_timer, PUBLISHING_INTERVAL);
	btn = button_hal_get_by_index(0);  //Returns the button of index0, since we only have one button

  	/* Main loop */
	while(1) {

		PROCESS_YIELD();

		if(ev == button_hal_press_event) {
			sensor_sample();
		}
		/*
		if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL){
			sensor_sample();
			// cambiare publishing interval con intero random tra 5 e 15
			etimer_set(&periodic_timer, PUBLISHING_INTERVAL);
		} */
	}

  PROCESS_END();
}
