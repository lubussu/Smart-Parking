# Smart Parking - IoT Project

Developed for the "Internet of Things" university course, the Smart Parking project aims to revolutionize urban parking. By leveraging IoT technologies, the system optimizes space usage, improves user experience, reduces traffic congestion, and minimizes environmental impact.

The system includes MQTT nodes as sensors, CoAP nodes as actuators, a Border Router, and a Collector connected to a database. Utilizing nRF52840 dongles, the nodes simulate various functionalities. Dongles generate random vibrations (within specified ranges) or simulate stable nitrogen dioxide levels based on button presses and LED feedback.

Actuators, also simulated with nRF dongles, include a bar actuator controlling traffic flow in and out of the parking lot and a fan actuator regulating air quality. The bar actuator's LED indicates the bars status (up or down), while the fan actuator's LED reflects different fan speeds based on nitrogen dioxide levels.
