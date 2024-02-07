#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

static char fan_state[] = "0";
static int max_speed = 3;

RESOURCE(res_fan,
         "title=\"Smart Parking: ?fan=0\";rt=\"Control\"", 
         res_get_handler,
         NULL,
         res_put_handler,
         NULL);


static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{

  int length = strlen(fan_state);
  memcpy(buffer, fan_state, length);
  
  coap_set_header_content_format(response, TEXT_PLAIN); /* text/plain is the default, hence this option could be omitted. */
  coap_set_header_etag(response, (uint8_t *)&length, 1);
  coap_set_payload(response, buffer, length);
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  if(request->payload_len > 0) {

    /* Get the payload data */
    const char* payload = (const char*)request->payload;
    printf("Received: %s\n", payload);

    int speed = atoi(payload);

    if(speed > max_speed){
      speed = max_speed;
    }else if(speed < 0){
      speed = 0;
    }

    strcpy(fan_state, payload);

    if(speed > 0){
      leds_off(14);
      leds_on(1 << speed);
    }else{
      leds_off(14);
    }

    /* Set the response code to indicate success */
    coap_set_status_code(response, CHANGED_2_04);

  } else{
    coap_set_status_code(response, BAD_REQUEST_4_00);
  }
}