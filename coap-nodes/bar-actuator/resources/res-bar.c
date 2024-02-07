#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "coap-engine.h"
#include "os/dev/leds.h"
#include "contiki.h"

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

char bars[2][5] = {"up", "down"};

RESOURCE(res_bar,
         "title=\"Smart Parking: ?bar=0\";rt=\"Control\"", 
         res_get_handler,                                 //GET HANDLER
         NULL,                                            //POST HANDLER                                            
         res_put_handler,                                 //PUT HANDLER
         NULL);                                           //DELETE HANDLER


static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){
  const char *bar = NULL;
  int index;
  int length;

  if(coap_get_query_variable(request, "bar", &bar)){
    index = atoi(bar);
    if(index != 0 && index != 1){
      return;
    }

    length = strlen(bars[index]);
    if (length < 0){
      length = 0;
    }
    if(length > REST_MAX_CHUNK_SIZE){
      length = REST_MAX_CHUNK_SIZE;
    }
    memcpy(buffer, bars[index], length);
  } else {
    char message[100]; 
    strcpy(message, bars[0]); 
    strcat(message, " "); 
    strcat(message, bars[1]); 
     
    length = strlen(message); 

    memcpy(buffer, message, length);
  }
  coap_set_header_content_format(response, TEXT_PLAIN);
  coap_set_header_etag(response, (uint8_t *)&length, 1);
  coap_set_payload(response, buffer, length);

}

static void close_out_bar(){
  leds_off(1);
  strcpy(bars[1], "down");
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){
  const char *bar; 
  if(coap_get_query_variable(request, "bar", &bar) && request->payload_len > 0) { 
    int index = atoi(bar);

    if(index != 0 && index != 1){ 
        return; 
    } 
   
    /* Get the payload data */
    const char* payload = (const char*)request->payload;
    printf("Received: %s\n", payload);

    /* Parse the payload to determine the LED state */
    if(strcmp(payload, "up") == 0) {
      /* Turn on the GREEN_LEDS */
      strcpy(bars[index], "up");

      if(index==0){ //entrance barrier
        leds_off(LEDS_RED);
        leds_on(LEDS_GREEN);
      } else if (index==1){ //out barrier
        static struct ctimer ct;
        leds_on(1);
        ctimer_set(&ct, 3 * CLOCK_SECOND, close_out_bar, NULL);
      }

    } else if(strcmp(payload, "down") == 0) {
      strcpy(bars[index], "down");
      if(index==0){ //entrance barrier
        leds_off(LEDS_GREEN);
        leds_on(LEDS_RED);
      }
    }

    /* Set the response code to indicate success */
    coap_set_status_code(response, CHANGED_2_04);
  } else {
    /* Set the response code to indicate bad request */
    coap_set_status_code(response, BAD_REQUEST_4_00);
  }

}