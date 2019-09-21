package com.teleonome.framework;

public class TeleonomeConstants {

	public static final String ADA_INTERNAL_HOST_IPADDRESS="172.16.1.1";
	
	//
	// the ports for the exoZeroNetwork
	//
	public static final int EXOZERO_PULSE_PORT=5563;
	public static final int ENDOZERO_PULSE_PORT=5565;
	
	public static final int EXOZERO_MNEMOSYNE_MANAGER_PORT=5564;

	//
	// the Computer Model values  (Internal:Descriptive:Computer Info:Computer Model
	//
	public static final String  COMPUTER_MODEL_PI_A ="Raspberry Pi Model A";
	public static final String  COMPUTER_MODEL_PI_B_REV_1_256="Raspberry Pi Model B (Rev 1.0, 256Mb)";
	public static final String  COMPUTER_MODEL_PI_B_256="Raspberry Pi Model B (Rev 2.0, 256Mb)";
	public static final String  COMPUTER_MODEL_PI_B_512="Raspberry Pi Model B (Rev 2.0, 512Mb)";
	public static final String  COMPUTER_MODEL_PI_A_PLUS ="Raspberry Pi Model A+";
	public static final String  COMPUTER_MODEL_PI_B_PLUS="Raspberry Pi Model B+";
	public static final String  COMPUTER_MODEL_PI_2_B="Raspberry Pi 2 Model B";
	public static final String  COMPUTER_MODEL_PI_3_MODEL_B="Raspberry Pi 3 Model B";
	
	//
	// the status for the Teleonomes
	//
	public static final String TELEONOME_STATUS_ACTIVE = "OK";
	public static final String TELEONOME_STATUS_DISCOVERED = "Discovered";
	public static final String TELEONOME_STATUS_MISSED_PULSE = "Missed Pulse";
	public static final String TELEONOME_STATUS_CAUTION = "Caution";
	public static final String TELEONOME_STATUS_ALERT = "Alert";
	//
	// if the status is Alert, the teleonome will put a Alert Message
	//
	public static final String TELEONOME_STATUS_ALERT_MESSAGE = "Alert Message";
	
	//
	// the status detail of a Teleonome
	//
	public static final String TELEONOME_OPERATION_MODE_UNKNOWN = "Unknown";
	public static final String TELEONOME_OPERATION_MODE_NORMAL = "Normal";
	public static final String TELEONOME_OPERATION_MODE_NO_WIFI = "No WiFi";
	public static final String TELEONOME_OPERATION_MODE_HIBERNATION = "Hibernation";	
	public static final String TELEONOME_OPERATION_NOT_AVAILABLE = "Not Available";
	
	//
	// the processor related variables
	//
	public static final String DENEOWRD_TYPE_PROCESSOR_POINTER = "Processor Pointer";
	public static final String DENE_TYPE_PROCESSOR = "Processor";
	public static final String ON_LACK_OF_DATA="On Lack of Data";
	
	//
	// the different kinds of deneword operations
	//
	public static final String DENE_TYPE_DENEWORD_OPERATION_EXPRESSION_EVALUATION = "Expression Evaluation";
	public static final String DENE_TYPE_DENEWORD_OPERATION_EXPRESSION_SWITCH = "Expression Switch";
	public static final String DENE_TYPE_DENEWORD_OPERATION_DATA_TRANSFORMATION = "Data Transformation";
	//
	// the types of microcontrolers
	//
	public static final String DENEWORD_TYPE_MICROCONTROLLER_TYPE="MicroController Type";
	public static final String MICROCONTROLLER_TYPE_SOFTWARE="Software";
	public static final String MICROCONTROLLER_TYPE_HARDWARE="Hardware";
	
	//
	// the value of the teleonome Identity
	//
	public static final String DENEWORD_TYPE_INITIAL_IDENTITY_MODE = "Initial Identity Mode";
	public static final String DENEWORD_TYPE_CURRENT_IDENTITY_MODE = "Current Identity Mode";
	
	public static final String USER_COMMAND="UserCommand";
	public static final String TELEONOME_IDENTITY_LABEL = "Identity";
	public static final String TELEONOME_IDENTITY_SELF = "Self";
	public static final String TELEONOME_IDENTITY_ORGANISM = "Organism";
	public static final String TELEONOME_IDENTITY_DUAL = "Dual";
	
	
	//
	// the data type for the values coming from Arduino
	//
	public static final String DATATYPE_DOUBLE = "double";
	public static final String DATATYPE_BOOLEAN = "boolean";
	
	public static final String DATATYPE_STRING = "String";
	public static final String DATATYPE_INTEGER = "int";
	public static final String DATATYPE_DENE_POINTER = "Dene Pointer";
	public static final String DATATYPE_TIMESTAMP = "Timestamp";
	public static final String DATATYPE_JSONARRAY = "JSONArray";
	public static final String DATATYPE_JSONOBJECT = "JSONObject";
	
	public static final String DATATYPE_IMAGE_FILE = "Image File";
	public static final String DATATYPE_AUDIO_FILE = "Audio File";
	public static final String DATATYPE_VIDEO_FILE = "Video File";
	
	public static final String  DATATYPE_TIMESTAMP_MILLISECONDS = "Timestamp Milliseconds";
	public static final String  WEBSERVER_PROCESS_AVAILABLE_MEMORY="Available Memory to the Webserver JVM";
	public static final String  WEBSERVER_PROCESS_MAXIMUM_MEMORY="Maximum Memory for the Webserver JVM";
	
	public static final String  HYPOTHALAMUS_PROCESS_AVAILABLE_MEMORY="Available Memory to the Hypothalamus JVM";
	public static final String  HYPOTHALAMUS_PROCESS_MAXIMUM_MEMORY="Maximum Memory for the Hypothalamus JVM";
	
	public static final String  HEART_PROCESS_AVAILABLE_MEMORY="Available Memory to the Heart JVM";
	public static final String  HEART_PROCESS_MAXIMUM_MEMORY="Maximum Memory for the Heart JVM";
	
	public static final String DATATYPE_LONG = "long";
	public static final String DATATYPE_FILE="File";
	public static final String DATATYPE_TIME_SERIES="Time Series";
	public static final String TIME_SERIES_MINIMUM="Minimum";
	public static final String TIME_SERIES_MAXIMUM="Maximum";
	public static final String TIME_SERIES_AVERAGE="Average";
	//
	// the different types of Denes
	//
	public static final String DENE_TYPE_SENSOR = "Sensor";
	public static final String DENE_TYPE_TELEPATHON = "Telepathon";
	public static final String DENE_TYPE_SENSOR_VALUE = "Sensor Value";
	public static final String DENE_TYPE_SENSOR_VALUE_DEFINITION = "Sensor Value Definition";
	public static final String SENSOR_VALUE_RANGE_MAXIMUM = "Range Maximum";
	public static final String SENSOR_VALUE_RANGE_MINIMUM = "Range Minimum";
	public static final String SENSOR_CURRENT_VALUE = "Current Value";
	
	
	
	public static final String DENE_TYPE_ON_START_ACTION = "On Start Action";
	public static final String DENE_TYPE_ON_START_SENSOR = "On Start Sensor";
	
	
	public static final String DENE_TYPE_ACTUATOR = "Actuator";
	public static final String DENE_TYPE_REPORT = "Report";
	public static final String DENE_TYPE_MICROCONTROLLER = "Microcontroller";
	public static final String DENE_TYPE_MICROCONTROLLER_CONFIG_PARAMETER_LIST = "Microcontroller Config Parameter List";
	public static final String DENE_TYPE_MICROCONTROLLER_CONFIG_PARAMETER = "Microcontroller Config Parameter";
	
	public static final String DENE_TYPE_VITAL = "Vital";
	public static final String DENE_TYPE_CODON_ELEMENT = "Codon Element";
	
	
	public static final String DENE_TYPE_HUMAN_INTERFACE_STATE_VARIABLES= "Human Interface State Variables";
	public static final String DENE_TYPE_ACTUATOR_ACTION_PROCESSING="Actuator Action Processing";
	public static final String DENE_TYPE_ACTUATOR_DENE_PROCESSING="Actuator Action Processing";
	public static final String DENE_TYPE_ACTUATOR_DENE_OPERATION_EVALUATION_PROCESSING="Actuator Action Dene Operation Evaluation Processing";
	
	
	public static final String DENE_TYPE_COMPONENT= "Component";
	public static final String DENE_TYPE_COMPONENT_TYPE= "Component Type";
	public static final String DENE_TYPE_COMPONENT_PROVIDER= "Component Provider";
	public static final String DENE_TYPE_COMPONENT_GROUP= "Component Group";
	public static final String DENE_TYPE_COMPONENT_LIST= "Component List";
	
	public static final String DENE_TYPE_COMPONENT_ELEMENT= "Component Group Elements List";
	public static final String DENEWORD_TYPE_COMPONENT_GROUP_LIST= "Component Group Elements List";
	public static final String DENEWORD_TYPE_COMPONENT_PROVIDER= "Component Provider";
	
	public static final String DENEWORD_ACTUATOR_COMMAND_CODE_TRUE_EXPRESSION= "Actuator Command Code True Expression";
	public static final String DENEWORD_ACTUATOR_COMMAND_CODE_FALSE_EXPRESSION= "Actuator Command Code False Expression";
	public static final String DENE_TYPE_CONDITION_DENOMIC_OPERATION="Condition Denomic Operation";
	public static final String DENE_TYPE_DENOMIC_OPERATION="Denomic Operation";

	public static final String DENEWORD_TYPE_EVENT_DATA_STRUCTURE="Event Data Structure";
	public static final String DENEWORD_TYPE_EVENT_MNEMOSYNE_DESTINATION="Event Mnemosyne Destination";
	
	public static final String EVENT_DATA_STRUCTURE_FLOWMETER="FlowMeter";
	
	
	//
	// the different types of Denomic Operatins
	public static final String DENOMIC_ELEMENT_EXISTS="Denomic Element Exists";
	public static final String DENOMIC_ELEMENT_DOES_NOT_EXISTS="Denomic Element Does Not Exists";
	public static final String CONDITION_DENOMIC_OPERATION_DATA_SOURCE="Data Source";
	public static final String CONDITION_DENOMIC_OPERATION="Operation";
	
	//
	// the different Denes
	//
	public static final String DENE_VITAL = "Vital";
	public static final String DENE_OPERATIONAL_PARAMETERS_THRESHOLDS = "Operational Parameters Thresholds";
	public static final String DENE_USB_DEVICES="USB Devices";
	public static final String DENE_PROCESSOR_INFO="Processor Info";
	public static final String DENE_COMPUTER_INFO="Computer Info";
	public static final String DENE_OPERON_CONTROL="Operon Control";
	public static final String DENE_POWER_STATUS="Power Status";
	public static final String DENEWORD_MAIN_BATTERY_VOLTAGE="MainBatteryVoltage";
	public static final String DENEWORD_MAIN_BATTERY_OUTPUT_CURRENT="MainBatteryOutputCurrent";
	public static final String DENEWORD_MAIN_BATTERY_INPUT_CURRENT="MainBatteryInputCurrent";
	public static final String DENEWORD_SOLAR_PANEL_INPUT_CURRENT="SolarPanelInputCurrent";
	public static final String DENEWORD_MAIN_BATTERY_STATE_OF_CHARGE="InternalBatteryStateOfCharge";
	
	//
	// the different types of DeneChain
	//
	public static final String DENECHAIN_OPERONS = "Operons";
	public static final String DENECHAIN_ACTUATORS = "Actuators";
	public static final String DENECHAIN_ANALYSIS = "Analysis";
	
	public static final String DENECHAIN_SENSORS = "Sensors";	
	public static final String DENECHAIN_TELEPATHONS = "Telepathons";    
	public static final String DENECHAIN_DESCRIPTIVE = "Descriptive";
	public static final String DENECHAIN_COMPONENTS = "Components";
	public static final String DENECHAIN_ANALYTICONS = "Analyticons";
	public static final String DENE_CONTROL_PARAMETERS = "Control Parameters";
	public static final String  DENECHAIN_MEDULA = "Medula";
	
	public static final String DENECHAIN_EXTERNAL_DATA = "External Data";
	public static final String DENECHAIN_ACTUATOR_LOGIC_PROCESSING ="Actuator Logic Processing";
	public static final String DENECHAIN_PATHOLOGY ="Pathology";
	public static final String DENECHAIN_MNEMOSYNE_PATHOLOGY ="Mnemosyne Pathology";
	public static final String DENECHAIN_MNEMOSYNE_PULSE ="Mnemosyne Pulse";

	public static final String DENECHAIN_HUMAN_INTERFACE = "Human Interface";
	
	public static final String DENECHAIN_OPERATIONAL_DATA ="Operational Data";
	public static final String DENECHAIN_SENSOR_DATA ="Sensor Data";
	
	public static final String DENE_NETWORK_SENSOR_STATUS ="Network Status";
	
	
	public static final String DENECHAIN_ORGANISM_CONFIGURATION="Organism Configuration";
	public static final String DENECHAIN_ORGANISM_STATUS="Organism Status";
	
	//
	// the different types of Nuclei
	//
	public static final String NUCLEI_INTERNAL = "Internal";
	public static final String NUCLEI_PURPOSE = "Purpose";
	public static final String NUCLEI_MNEMOSYNE = "Mnemosyne";
	public static final String NUCLEI_HUMAN_INTERFACE = "Human Interface";
	
	public static final String VALUE_UNDEFINED = "Undefined";
	
	//
	// the Vital Dene DeneWords
	//
	public static final String VITAL_DENEWORD_BASE_PULSE_FREQUENCY = "Base Pulse Frequency";
	public static final String VITAL_DENEWORD_TIMEZONE = "Timezone";
	public static final String VITAL_DENEWORD_INTER_SENSOR_READ_TIMEOUT_MILLISECONDS = "Inter Sensor Read Timeout Milliseconds";
	public static final String VITAL_DENEWORD_PERSIST_DATA="Persist Data";
	
	//
	// the command embedded as values
	//
	public static final String COMMANDS_CREATE_DAILY_PARTITIONS="$CreateDailyPartitions";
	public static final String COMMANDS_ELAPSED_TIME_STRING = "$ElapsedTimeString";
	public static final String COMMANDS_PUBLISH_TELEONOME_PULSE="$Teleonome_Pulse";
	public static final String COMMANDS_CURRENT_HOUR = "$Current_Hour";
	public static final String COMMANDS_CURRENT_DATE = "$Current_Date";
	public static final String COMMANDS_CURRENT_TIMESTAMP = "$Current_Timestamp";
	public static final String COMMANDS_CURRENT_TIMESTAMP_MILLIS = "$Current_Timestamp_Millis";
	public static final String COMMANDS_CURRENT_MINUTE_IN_THE_HOUR = "$Current_Minute_In_The_Hour";
	public static final String COMMANDS_CURRENT_HOUR_IN_DAY = "$Current_Hour_In_Day";
	public static final String COMMANDS_CURRENT_DAY_IN_WEEK = "$Current_Day_In_Week";
	public static final String COMMANDS_CURRENT_DAY_IN_MONTH = "$Current_Day_In_Month";
	public static final String COMMANDS_GENERATE_DIGITAL_GEPPETTO_CODE = "$Generate_Digital_Geppetto_Code";
	public static final String CURRENT_TIMESTAMP_VARIABLE_NAME = "CurrentTime";
	
	
	public static final String COMMANDS_TIME_FOR_LCD = "$TimeForLCD";
	public static final String COMMANDS_DATE_FOR_LCD = "$DateForLCD";
	public static final String COMMANDS_DATE_FOR_EXPORT_FILENAME="$Date_For_File_Export";

	public static final String COMMANDS_IP_ADDRESS_FOR_LCD = "$IPForLCD";
	public static final String COMMANDS_SSID_FOR_LCD = "$SSIDLCD";
	
	public static final String COMMANDS_SET_MICROCONTROLLER_RTC = "$SetMicrocontrollerTime";
	public static final String COMMANDS_DO_NOTHING = "$DoNothing";
	public static final String COMMAND_MNEMOSYNE_LAST_DENE_POSITION="$Last_Dene_Position";
	public static final String COMMAND_MNEMOSYNE_PREVIOUS_TO_LAST_DENE_POSITION="$Previous_To_Last_Dene_Position";
	public static final String COMMAND_MNEMOSYNE_FIRST_POSITION="$First_Position";
	
	public static final String COMMANDS_PREVIOUS_PULSE_VALUE = "$Previous_Pulse_Value";
	public static final String COMMANDS_PREVIOUS_PULSE_TIMESTAMP = "$Previous_Pulse_Timestamp";
	public static final String COMMANDS_PREVIOUS_PULSE_MILLIS = "$Previous_Pulse_Millis";
	
	public static final String COMMAND_REBOOT="$Reboot";
	public static final String COMMAND_SHUTDOWN="$Shutdown";
	public static final String COMMAND_REBOOT_TEXT="Reboot";
	public static final String COMMAND_SHUTDOWN_TEXT="Shutdown";
	
	public static final String SHUTDOWN_ACTION="Shutdown Action";
	
	public static final String COMMAND_SHUTDOWN_ENABLE_HOST="Shutdown Enable Host";
	public static final String COMMAND_SHUTDOWN_ENABLE_NETWORK="Shutdown Enable Network";
	public static final String COMMAND_REBOOT_ENABLE_HOST="Reboot Enable Host";
	public static final String COMMAND_REBOOT_ENABLE_NETWORK="Reboot Enable Network";
	public static final String COMMAND_KILL_PULSE="KillPulse";
	public static final String COMMAND_CONFIRM_REBOOT="ConfirmReboot";
	public static final String COMMAND_CONFIRM_SHUTDOWN="ConfirmShutdown";
	public static final String COMMAND_START_PULSE="StartPulse";
	public static final String COMMAND_FAULT="Fault";
	public static final String COMMAND_LOCATION_WEBSERVER_ROOT="$Webserver_Root";

	
	public static final String COMMAND_CONFIRM_STOP_PULSE="ConfirmKillPulse";
	public static final String COMMAND_REBOOTING="Rebooting";
	public static final String COMMAND_SHUTINGDOWN="Shutingdown";
	public static final String COMMAND_STOPPING_PULSE="StoppingPulse";
	public static final String COMMAND_STATE_MUTATION_PAYLOAD_INJECTION = "State Mutation Payload Injection";
	
	public static final String COMMAND_VERIFY_FILE_CREATION_DATE = "$Verify_File_Creation_Date";
	public static final String COMMAND_VERIFY_FILE_MODIFICATIN_DATE = "$Verify_File_Creation_Date";
	
	
	
	public static final String CAMERA_TIMESTAMP_FILENAME_FORMAT="dd-MM-yyyy_HHmm";
	
	//
	// the mutations denechains
	//
	public static final String DENECHAIN_ON_LOAD_MUTATION = "On Load";
	public static final String DENECHAIN_ON_FINISH_MUTATION = "On Finish";
	public static final String DENECHAIN_DENE_INJECTION ="Dene Injections";
	public static final String DENECHAIN_DENEWORD_INJECTION ="DeneWord Injections";
	public static final String DENECHAIN_DENE_DELETION ="Dene Deletion";
	public static final String DENECHAIN_DENEWORD_DELETION ="DeneWord Deletion";
	
	public static final String DENECHAIN_ACTIONS_TO_EXECUTE="Actions To Execute";
	public static final String DENECHAIN_MNEMOSYCONS_TO_EXECUTE="Mnemosycons To Execute";
	public static final String DENECHAIN_MNEMOSYNE_OPERATIONS="Mnemosyne Operations";
	public static final String DENE_TYPE_MNEMOSYCON_LIST="Mnemosycon List";
	
	
	public static final String DENE_TYPE_DENEWORD_CARRIER= "DeneWord Carrier";
	public static final String DENE_TYPE_ACTION_LIST= "Action List";
	public static final String DENE_NAME_ATTRIBUTE= "Name";
	
	//
	// the mutation actions dene types
	//
	public static final String MUTATION_COMMAND_SET_DENEWORD = "Set DeneWord";
	public static final String MUTATION_TARGET = "Target";
	public static final String MUTATION_PAYLOAD_VALUE="Mutation Payload Value";
	public static final String MUTATION_PAYLOAD_VALUETYPE="Mutation Payload Value Type";
	
	public static final String MUTATION_INJECTION_TARGET = "Injection Target";
	public static final String MUTATION_DELETION_TARGET = "Deletion Target";
	public static final String MUTATION_PAYLOAD_UPDATE_TARGET = "Payload Update Target";
	
	public static final String MUTATION_COMMAND_DENEWORD_NAME = "Mutation Name";
	public static final String MUTATION_COMMAND_DENEWORD_VALUE = "Mutation Value";
	//
	// the type of mutatins according to when they get executed
	public static final String MUTATION_EXECUTION_MODE_IMMEDIATE="Immediate";
	public static final String MUTATION_EXECUTION_MODE_NORMAL="Normal";
	
	public static final String MUTATION_PROCESSING_LOGIC_DENE_CHAIN_NAME="Mutation Processing Logic";
	
	
	//
	// the mutation type
	//
	public static final String MUTATION_TYPE_STATE="State";
	public static final String MUTATION_TYPE_STRUCTURE="Structure";
	public static final String MUTATION_TYPE_ATTRIBUTE = "Mutation Type";
	  
	public static final String MUTATION_INVOCATION_MODE_ATTRIBUTE="Invocation Mode";
	public static final String MUTATION_INVOCATION_MODE_EVENT="Event";
	public static final String MUTATION_INVOCATION_MODE_TIME = "Time";

	  
	public static final String DENE_IDENTITY_SWITCH_CONTROL_PARAMETERS = "Identity Switch Control Parameters";
	public static final String DENE_TYPE_IDENTITY_SWITCH_EVENT = "Identity Switch Event";
	public static final String IDENTITY_SWITCH_EVENT_TIMESTAMP = "Identity Switch Event Timestamp";
	public static final String IDENTITY_SWITCH_EVENT_MILLISECONDS =  "Identity Switch Event Milliseconds";

	public static final String DENE_TYPE_EXOGENUS_METAMORPHOSIS_EVENT = "Exogenous Metamorphosis Event";
	
	public static final String DENEWORD_MAXIMUM_NUMBER_REBOOTS_BEFORE_IDENTITY_SWITCH = "Maximum Number Reboots Before Identity Switch";
	public static final String DENEWORD_CURRENT_NUMBER_REBOOTS_BEFORE_METAMORPHOSIS = "Current Number Reboots Before Metamorphosis";
	public static final String DENEWORD_COMPUTER_MODEL = "Computer Model";
	public static final String DENEWORD_CURRENT_NUMBER_REBOOTS_BEFORE_IDENTITY_SWITCH = "Current Number Reboots Before Identity Switch";
	public static final String DENEWORD_IDENTITY_SWITCH_EVENTS_MNEMOSYNE_DESTINATION = "Identity Switch Events Mnemosyne Destination";
	public static final String METAMORPHOSIS_EVENT_TIMESTAMP = "Metamorphosis Event Timestamp";
	public static final String METAMORPHOSIS_EVENT_MILLISECONDS =  "Metamorphosis Event Milliseconds";
	
	
	//
	// Deneword types
	//
	public static final String DENEWORD_TYPE_MICROCONTROLLER_CONFIG_PARAM_LIST = "Microcontroller Config Parameter List";
	public static final String DENEWORD_TYPE_ACTION="Action";
	public static final String DENEWORD_TYPE_CURRENT_PULSE="Current Pulse";
	public static final String DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY="Current Pulse Frequency";
	public static final String DENEWORD_TYPE_CURRENT_PULSE_GENERATION_DURATION="Current Pulse Generation Duration";
	public static final String DENEWORD_TYPE_CONFIGURATION_PARAMETER="Configuration Parameter";
	
	public static final String DENEWORD_TYPE_BASE_PULSE_FREQUENCY="Base Pulse Frequency";
	public static final String DENEWORD_TYPE_NUMBER_PULSES_BEFORE_LATE="Number of Pulses Before Late";
	public static final String DENEWORD_TYPE_MICROCONTROLLER_FAULT_PATHOLOGY_MNEMOSYNE_LOCATION="'Fault Pathology Mnemosyne Location";
	
	public static final String DENEWORD_TYPE_COMPONENT_POINTER = "Component Pointer";
	public static final String DENEWORD_TYPE_MNEMOSYCON_POINTER = "Mnemosycon Pointer";
	public static final String DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER = "Actuator Condition Pointer";
	public static final String DENEWORD_TYPE_CONDITION_VARIABLE_POINTER = "Condition Variable Pointer";
	public static final String DENEWORD_TYPE_SENSOR_VALUE = "Sensor Value";
	public static final String DENEWORD_TYPE_UPDATE_DENEWORD_VALUE = "Update DeneWord Value";
	public static final String DENEWORD_TYPE_APPEND_DENEWORD_VALUE = "Append DeneWord Value";
	public static final String DENE_TYPE_UPDATE_DENEWORD_VALUE = "Update DeneWord Value";
	public static final String DENE_TYPE_UPDATE_DENE_NAME = "Update Dene Name";
	
	
	public static final String DENEWORD_TYPE_FIRMWARE_COMMAND_POINTER = "Firmware Command Pointer";
	public static final String DENEWORD_TYPE_FIRMWARE_COMMAND = "Firmware Command";
	public static final String DENEWORD_TYPE_SENSOR_MICROCONTROLLER_POINTER = "Sensor Microcontroller Pointer";
	public static final String DENEWORD_TYPE_TELEPATHON_MICROCONTROLLER_POINTER = "Telepathon Microcontroller Pointer";
	public static final String DENEWORD_TYPE_TELEPATHON_EVENT_LIST_POINTER = "Event List Pointer";
	public static final String DENEWORD_TYPE_EVENT_DEFINITION = "Event Definition";
	public static final String DENEWORD_TYPE_EVENT_DATA_STRUCTURE_VALUE_LIST = "Event Data Structure Value List";
	public static final String DENEWORD_TYPE_EVENT_VALUE_DEFINITION = "Event Value Definition";
	public static final String DENEWORD_EVENT_STRING_QUEUE_POSITION = "Event String Queue Position";
	public static final String DENEWORD_NUMBER_OF_SAMPLES_IN_EVENT="Number Of Samples In Event";
	
	
	public static final String DENEWORD_TYPE_TELEPATHON_TYPE = "Telepathon Type";
	public static final String DENEWORD_TYPE_TELEPATHON_COMMUNCATION_PROFILE = "Communication Profile Pointer";
	
	public static final String TELEPATHON_TYPE_I2C = "I2C Telepathon";
	public static final String TELEPATHON_TYPE_XBEE = "XBee Telepathon";
	public static final String TELEPATHON_TYPE_RS485 = "RS485 Telepathon";
	public static final String TELEPATHON_TYPE_SPI = "SPI Telepathon";
	
	public static final String DENEWORD_TYPE_MICROCONTROLLER_PORT="Microcontroller Port";
	public static final String DENEWORD_TYPE_MICROCONTROLLER_PORT_TYPE="Microcontroller Port Type";
	public static final String DENEWORD_TYPE_MICROCONTROLLER_ANALOG_PORT="Analog";
	public static final String DENEWORD_TYPE_MICROCONTROLLER_DIGITAL_PORT="Digital";
	public static final String DENEWORD_TYPE_ACTUATOR_MICROCONTROLLER_POINTER = "Actuator Microcontroller Pointer";
	public static final String DENEWORD_TYPE_ACTUATOR_POINTER = "Actuator Pointer";
	public static final String DENEWORD_TYPE_ACTION_SUCCESS_TASKS="Action Success Tasks";
	public static final String DENEWORD_TYPE_ACTION_SUCCESS_TASK_TRUE_EXPRESSION="Action Success Tasks True Expression";
	public static final String DENEWORD_TYPE_ACTION_SUCCESS_TASK_FALSE_EXPRESSION="Action Success Tasks False Expression";
	public static final String DENEWORD_TYPE_MNEMOSYNE_OPERATION_FALSE_EXPRESSION="Mnemosyne Operations False Expression";
	public static final String DENEWORD_TYPE_MNEMOSYNE_OPERATION_TRUE_EXPRESSION="Mnemosyne Operations True Expression";
	public static final String DENEWORD_TYPE_MNEMOSYNE_OPERATION="Mnemosyne Operation";
	public static final String MNEMOSYNE_PRUNNING_STRATEGY_RESET="Reset";
	public static final String MNEMOSYNE_PRUNNING_STRATEGY_ERASE="Erase";
	public static final String MNEMOSYNE_PRUNNING_STRATEGY="Mnemosyne Prunning Strategy";

	public static final String ACTION_SUCCESS_TASK_LABEL = "ActionSuccessTasks";
	public static final String MNEMOSYNE_OPERATION_INDEX_LABEL = "Mnemosyne Operation Index";
	public static final String DENE_TYPE_MNEMOSYNE_OPERATION_UPDATE_TIMESERIES_COUNTER="Mnemosyne Update Time Series Counter";
	public static final String DENE_TYPE_MNEMOSYNE_OPERATION_UPDATE_COUNTER="Mnemosyne Update Counter";
	public static final String DENE_TYPE_MNEMOSYNE_OPERATION_RESET_COUNTER="Mnemosyne Reset Counter";
	
	public static final String DENEWORD_TYPE_MNEMOSYNE_OPERATION_COUNTER_POINTER="Counter Pointer";
	public static final String DENEWORD_TYPE_COUNTER_LIMIT="Counter Limit";
	public static final String DENEWORD_TYPE_COUNTER_INCREMENT="Counter Increment";
	
	public static final String DENE_TYPE_MNEMOSYNE_OPERATION_CONVERT_DENEWORDS_TO_JSONARRAY="Mnemosyne Convert DeneWords to JSONArray";
	public static final String DENE_TYPE_MNEMOSYNE_OPERATION_COPY_TIMESERIES_ELEMENT_TO_TIMESERIES="Mnemosyne Copy Time Series Element to Time Series";
	public static final String DENE_TYPE_MNEMOSYNE_OPERATION_COPY_TIMESERIES_ELEMENT_SELECTOR_MAXIMUM_VALUE="Time Series Element Select Maximum";
	public static final String DENE_TYPE_MNEMOSYNE_OPERATION_COPY_TIMESERIES_ELEMENT_SELECTOR_MINIMUM_VALUE="Time Series Element Select Minimum";
	public static final String DENE_TYPE_MNEMOSYNE_OPERATION_COPY_TIMESERIES_ELEMENT_SELECTOR_AVERAGE_VALUE="Time Series Element Select Average";
	public static final String  DENEWORD_TYPE_TIMESERIES_ELEMENT_SELECTOR="Time Series Element Selector";
	
	public static final String  COMPLETE_TIMESERIES_ELEMENT="Complete Time Series Element";
	public static final String  TIMESERIES_TIMESTAMP="Time Series Element Timestamp";
	public static final String  TIMESERIES_VALUE="Time Series Element Value";

	
	public static final String DENEWORD_TYPE_DATA_SOURCE_POINTER="Data Source Pointer";
	public static final String DENEWORD_TYPE_NAVBAR_POINTER="NavBar Pointer";
	public static final String DENEWORD_TYPE_NAVBAR_POSITION="NavBar Position";
	public static final String DENEWORD_TYPE_NAVBAR_TEXT="NavBar Text";
	
	
	public static final String DENEWORD_TYPE_TIMESERIES_DATA_SOURCE_POINTER="Time Series Data Source Pointer";
	public static final String DENEWORD_TYPE_TIMESERIES_COUNTER_LIMIT="Time Series Counter Limit";
	public static final String DENEWORD_TYPE_TIMESERIES_DATA_POINTER="Time Series Data Pointer";
	public static final String DENEWORD_TYPE_TIMESERIES_COUNTER_POINTER="Time Series Counter Pointer";
	
	public static final String DENEWORD_TYPE_ANALYTICON_DATA_SOURCE="Analyticon Data Source";
	public static final String DENEWORD_TYPE_ANALYTICON_PROFILE_POINTER="Analyticon Profile Pointer";
	public static final String DENEWORD_TYPE_ON_START_ACTION_LIST= "On Start Action List";
	public static final String DENEWORD_TYPE_OPERATION_VARIABLE= "DeneWord Operation Variable";
	public static final String DENEWORD_TYPE_OPERATION_DESTINATION= "DeneWord Operation Destination";
	public static final String DENEWORD_TYPE_TRANSFORMATION_FUNCTION= "Transformation Function";
	public static final String DENEWORD_MOTHER_MICROCONTROLER="Mother Microcontroller";
	public static final String DENEWORD_ACTION_EXECUTION_POINT= "Execution Point";
	public static final String DENEWORD_ACTION_EXECUTION_POINT_IMMEDIATE= "Execution Point Immediate";
	public static final String DENEWORD_ACTION_EXECUTION_POINT_POST_PULSE= "Execution Point Post Pulse";
	public static final String TELEONOME_SECURITY_CODE="Teleonome Code";	
	public static final String DIGITAL_GEPPETTO_SECURITY_CODE="Digital Geppetto Code";	
	public static final String DENEWORD_CODE_TYPE="Code Type";
	
	public static final String DENEWORD_TYPE_DENEWORD_EXPORT_POINTER="Export DeneWord Pointer";
	public static final String DENEWORD_TYPE_DENEWORD_EXPORT_COLUMN_POSITION="Export DeneWord Column Position";
	public static final String EXPORT_OPERATION_CREATE="Export Create";
	public static final String EXPORT_OPERATION_APPEND="Export Append";

	public static final String DENE_TYPE_DENECHAIN_VISUALIZATION_POSITION = "DeneChain Visualization Position";
	public static final String DENE_TYPE_DENE_VISUALIZATION_POSITION="Dene Visualization Position";
	public static final String DENEWORD_TYPE_DENEWORD_VISUALIZATION_POSITION="DeneWord Visualization Position";
	
	public static final String DENEWORD_STATUS="Status";
	public static final String DENEWORD_OPERATIONAL_MODE="OperationalMode";
	public static final String OPERATIONAL_MODE_NORMAL="Normal";
	public static final String OPERATIONAL_MODE_WPS="WPS";
	
	
	public static final String OPERATIONAL_MODE_COMMA="Comma";
	
	public static final String DENEWORD_TYPE_EVALUATED_VARIABLE="Evaluated Variable";
	public static final String DENEWORD_SENSOR_REQUEST_QUEUE_POSITION="Sensor Request Queue Position";
	public static final String DENEWORD_REPORTING_ADDRESS="Reporting Address";
	public static final String DENEWORD_OPERATIONAL_STATUS_RED_VALUE="Operational Status Red Value";
	public static final String DENEWORD_OPERATIONAL_STATUS_GREEN_VALUE="Operational Status Green Value";
	public static final String DENEWORD_OPERATIONAL_STATUS_BLUE_VALUE="Operational Status Blue Value";
	public static final String DENEWORD_OPERATIONAL_STATUS_BLINK_VALUE="Operational Status Blink Value";
	public static final String DENEWORD_OPERATIONAL_STATUS_BOOTSTRAP_EQUIVALENT="Operational Status Bootstrap Equivalent";
	public static final String BOOTSTRAP_CRISIS="crisis";
	public static final String BOOTSTRAP_DANGER="danger";
	public static final String BOOTSTRAP_WARNING="warning";
	public static final String BOOTSTRAP_SUCCESS="success";
	public static final String BOOTSTRAP_PRIMARY="primary";
	public static final String DENEWORD_PULSE_SIZE_KB="Pulse Size Kb";
	public static final String EVALUATION_POSITION="Evaluation Position";
	
	//
	// time related variables
	//
	public static final String  DENEWORD_TYPE_TIME_BASED_MUTATION_EXECUTION_TIME="Execution Time";

	public static final String TIME_MIDNIGHT="Midnight";
	public static final String TIME_Start_WEEK="StartWeek";
	
	public static final String TIME_DAY_BEGINNING_DAYLIGHT="Day Beginning Daylight";
	public static final String TIME_DAY_BEGINNING_CHARGING="Day Beginning Charging Main Battery";
	public static final String TIME_DAY_END_DAYLIGHT="Day End Daylight Label";
	public static final String TIME_DAY_END_CHARGING="Day End Charging Main Battery";
	
	public static final String TIME_DAY_BEGINNING_DAYLIGHT_MILLISECONDS="Day Beginning Daylight Milliseconds";
	public static final String TIME_DAY_BEGINNING_CHARGING_MILLISECONS="Day Beginning Charging Main Battery Milliseconds";
	public static final String TIME_DAY_END_DAYLIGHT_MILLISECONS="Day End Daylight Milliseconds";
	public static final String TIME_DAY_END_CHARGING_MILLISECONS="Day End Charging Main Battery Milliseconds";

	
	
	
	
	public static final String DENEWORD_ACTION_PROCESSING_RESULT="Action Processing Result";
	public static final String DENEWORD_CONDITION_PROCESSING_RESULT="Condition Processing Result";
	public static final String DENEWORD_ACTION_EXPRESSION="Action Expression";
	public static final String DENEWORD_EXPRESSION="Expression";
	
	public static final String DENEWORD_CONDITION_EXPRESSION="Condition Expression";
	public static final String CONDITION_NAME="Condition Name";
	
	public static final String DENEWORD_COMMAND_TO_EXECUTE = "Command To Execute";
	public static final String DENE_PATHOLOGY ="Pathology";
	public static final String DENEWORD_ACTIVE="Active";
	public static final String DENEWORD_VISIBLE="Visible";
	
	public static final String DENEWORD_TYPE_POINTER="Dene Pointer";
	//
	// dene types
	//
	public static final String DENE_TYPE_ACTION="Action";
	public static final String DENE_TYPE_ACTUATOR_CONDITION = "Actuator Condition";
	public static final String DENE_TYPE_ACTUATOR_CONDITION_PROCESSING = "Actuator Condition Processing";
	public static final String DENE_TYPE_PROCESS_MEMORY_INFO="Process Memory Info";
	//
	// specific denewords
	//
	public static final String DENEWORD_MICROCONTROLLER_COMMUNICATION_PROTOCOL="Microcontroller Communication Protocol";
	//
	// constant used in the manager to mark whether to return the complete deneword or dene or just an attribute of it
	// if only an attribute is required, it is passed as text
	//
	public static final String COMPLETE = "Complete";
	//
	// control infon the lcd
	//
	public static final String LCD_DEBUG_ON_MODE = "DebugOn";
	public static final String LCD_DEBUG_OFF_MODE = "DebugOff";
	public static final String EXPIRATION_SECONDS = "Expiration Seconds";
	
	//
	// constants that represent the different attribute keys in a 
	// deneword.  used in searchs to specify what to return from a deneword
	// search when COMPLETE is not used
	//
	public static final String DENEWORD_VALUETYPE_ATTRIBUTE="Value Type";
	public static final String DENEWORD_COLOR_ATTRIBUTE="Color";
	public static final String DENEWORD_SERIESTYPE_ATTRIBUTE="Series Type";
	
	public static final String DENEWORD_VALUE_SET_ATTRIBUTE="Value Set";
	
	public static final String DENEWORD_DATA_SOURCE_ATTRIBUTE="Data Source";
	public static final String DENEWORD_DATA_LOCATION_ATTRIBUTE="Data Location";
	
	public static final String DENEWORD_SENSORS_ACTIVE = "Sensors Active";
	public static final String DENEWORD_ACTUATORS_ACTIVE = "Actuators Active";
	public static final String DENEWORD_ANALYTICONS_ACTIVE = "Analyticons Active";
	public static final String DENEWORD_MNEMOSYCONS_ACTIVE = "Mnemosycons Active";
	

	public static final String DENEWORD_DEFAULT_VALUE = "Default";
	public static final String DENEWORD_VALUE_ATTRIBUTE = "Value";
	public static final String DENEWORD_IDENTITY_ATTRIBUTE = "Identity";
	public static final String DENEWORD_QUANTITY_ATTRIBUTE = "Quantity";
	
	public static final String DENEWORD_TARGET_ATTRIBUTE = "Target";
	public static final String DENEWORD_TIMESTRING_VALUE = "Time String";
	public static final String DENEWORD_TIMESTRING_FORMAT_VALUE = "Time String Format";
	
	public static final String DENEWORD_UNIT_ATTRIBUTE = "Units";
	public static final String DENEWORD_NAME_ATTRIBUTE = "Name";
	public static final String DENEWORD_MAXIMUM_ATTRIBUTE = "Maximum";
	public static final String DENEWORD_MINIMUM_ATTRIBUTE = "Minimum";
	public static final String DENEWORD_AVERAGE_ATTRIBUTE = "Average";
	
	
	public static final String DENEWORD_REQUIRED_ATTRIBUTE = "Required";
	public static final String DENEWORD_DENEWORD_TYPE_ATTRIBUTE = "DeneWord Type";
	
	
	public static final String DENE_DENE_NAME_ATTRIBUTE = "Name";
	public static final String DENE_DENE_TYPE_ATTRIBUTE = "Dene Type";
	public static final String DENE_TYPE_EXTERNAL_DATA_SOURCE = "External Data Source";
	//
	// the pulse_status
	//
	public static final String PULSE_STATUS_FINISHED = "FinishedPulse";
	public static final String PULSE_STATUS_STARTED="Started Pulse";
	
	//
	// commands known to the commandServer
	//
	public static final String COMMAND_INITIATING_PULSE="Initiating Pulse";
	public static final String COMMAND_PULSE_COMPLETED="Pulse Completed";
	public static int COMMAND_SERVER_PORT=7777;
	public static final String COMMAND_SERVER_IP_LOCALHOST="127.0.0.1";
	public static final String COMMAND_QUEUE_EMPTY="Command Queue Empty";
	//
	// THE MAPPED BUS parameters
	//
	public static final String MAPPED_BUS_FILE="PulseGenerator";
	public static final String MAPPED_BUS_FILE_PULSE_STATUS="PulseStatus";
	public static long MAPPED_BUS_FILE_PULSE_STATUS_SIZE=3000000L;
	
	public static final String STARTING_PULSE_MAPPED_BUS_MESSAGE="Started Pulse";
	public static final String PULSE_FINISHED_MAPPED_BUS_MESSAGE="Pulse Finished";
	public static final String PULSE_SIZE_MESSAGE="Pulse Size";
	public static final String PULSE_TIMESTAMP="Pulse Timestamp";
	public static final String PULSE_TIMESTAMP_MILLISECONDS="Pulse Timestamp in Milliseconds";
	public static final String PULSE_CREATION_DURATION_MILLIS="Pulse Creation Duration Millis";

	//
	// The record size must be at least big enough to contain a pulse
	//
	public static int MAPPED_BUS_RECORD_SIZE=300000;
	public static final String DENEWORD_TYPE_MICROCONTROLLER_PROCESSING_CLASSNAME="Microcontroller Processing Class Name";
	public static final String DENEWORD_MICROCONTROLLER_ASYNC_REQUEST_DELAY_MILLIS="Async Request Delay Millis";
	
	public static final String COMMAND_REQUEST_NOT_EXECUTED="Not Executed";
	public static final String COMMAND_REQUEST_EXECUTED="Executed";
	public static final String COMMAND_REQUEST_SKIPPED_AT_INIT="Skipped at init";
	public static final String COMMAND_EXECUTED_ON="ExecutedOn";
	public static final String COMMAND_EXECUTION_STATUS="Status";
	public static final String COMMAND_CODE="CommandCode";
	public static final String COMMAND_REQUEST_EXECUTION_OK="Command Request Succesfull";
	
	public static final String MOTHER_COMMAND_REBOOT_HYPOTHALAMUS="RebootHypothalamus";
	public static final String MOTHER_COMMAND_REBOOT_HYPOTHALAMUS_OK="RebootHypothalamusOK";
	public static final String MOTHER_COMMAND_SHUTDOWN_HYPOTHALAMUS="ShutdownHypothalamus";
	public static final String MOTHER_COMMAND_SHUTDOWN_HYPOTHALAMUS_OK="RebootHypothalamusNotOK";
	public static final String MOTHER_INVALIDATED_REBOOT="Mother Invalidated Reboot";
	public static final String MOTHER_INVALIDATED_SHUTDOWN="Mother Invalidated Shutdown";
	
	public static final String COMMAND_REQUEST_INVALID_CODE="Failure-Invalid Code";
	public static final String COMMAND_REQUEST_VALID_CODE="Ok-Valid Code";
	public static final String COMMAND_REQUEST_PENDING_EXECUTION="Pending Execution";
	
	//
	// the index for the front end
	//
	public static final String TODAY_INDEX="TodayIndex";
	public static final String LAST_24_HOURS_INDEX="Last24HoursIndex";
	
	public static final String CODON="Codon";
	public static final String TIMER_FINISHED = "TimerFinished";
	
	//
	// the different types of processing to be done
	// by the Mnemosyne
	//
	public static final String DENE_TYPE_MNEMOSYNE_PROCESSING_INFO="Mnemosyne Processing Info";
	public static final String MNEMOSYNE_DENE_WPS_CYCLE_PULSE_COUNT="WPS Cycle Pulse Count";
	public static final String DENEWORD_TYPE_MNEMOSYNE_ANALYSIS="Mnemosyne Analysis";
	public static final String DENE_TYPE_ANALYTYCON="Analyticon";
	public static final String DENEWORD_MNEMOSYNE_ANALYSIS_PROFILE="Mnemosyne Analysis Profile";
	public static final String DENEWORD_MNEMOSYNE_COUNTER="Counter";
	public static final String DENEWORD_TYPE_MNEMOSYNE_PROCESSING_INTERVAL="Mnemosyne Processing Interval";
	public static final String DENEWORD_TYPE_MNEMOSYNE_PROCESSING_INTERVAL_START="Mnemosyne Processing Interval Start";
	public static final String DENEWORD_TYPE_MNEMOSYNE_PROCESSING_INTERVAL_END="Mnemosyne Processing Interval End";
	public static final String MNEMOSYNE_RECURRENCE_ONE_TIME="One Time";
	public static final String MNEMOSYNE_RECURRENCE_RECURRENT="Recurrent";
	public static final String SORTING_ORDER_DESCENDING="Descending";
	public static final String SORTING_ORDER_ASCENDING="Ascending";
	public static final String SORTING_PARAMETER="Sorting Parameter";
	public static final String SORTING_ORDER="Sorting Order";
	
	public static final String DENEWORD_TYPE_MNEMOSYNE_PROCESSING_INTERVAL_TYPE="Mnemosyne Processing Interval Type";
	public static final String MNEMOSYNE_PROCESSING_INTERVAL_TODAY="Today";
	public static final String MNEMOSYNE_PROCESSING_INTERVAL_TYPE_RANGE="Range";

	//
	// all the functions in the mnemosyne
	// no spaces allowed and use function style
	// it goes in the "Value" attribute of a deneword
	// ie 
	// deneWord.put("Value", TeleonomeConstants.MNEMOSYNE_PROCESSING_FUNCTION_RANGE_EVALUATION);
	
	//
	public static final String MNEMOSYNE_PROCESSING_FUNCTION_RANGE_EVALUATION="evaluateRange";
	public static final String MNEMOSYNE_PROCESSING_SAMPLE_FREQUENCY= "Mnemosyne Sample Frequency Seconds";
	public static final String MNEMOSYNE_PROCESSING_SAMPLE_TYPE="Mnemosyne Data Sample Type";
	
	public static final String MNEMOSYNE_PROCESSING_NOW="Now";
	public static final String DENEWORD_TYPE_MNEMOSYNE_DATA_RANGE="Mnemosyne Data Range";
	public static final String MNEMOSYNE_GRAPH_DISPLAY_ALL_VALUES="All Values";
	public static final String MNEMOSYNE_ANALYSIS_MAXIMUM_MINIMUM="Maximum and Minimum";
	public static final String MNEMOSYNE_ANALYSIS_MAXIMUM="Maximum";
	public static final String MNEMOSYNE_ANALYSIS_MINIMUM="Minimum";
	public static final String MNEMOSYNE_ANALYSIS_AVERAGE="Average";
	
	public static final String MNEMOSYNE_DENECHAIN_CURRENT_HOUR="Mnemosyne Current Hour";
	public static final String MNEMOSYNE_DENECHAIN_CURRENT_DAY="Mnemosyne Today";
	public static final String MNEMOSYNE_DENECHAIN_YESTERDAY="Mnemosyne Yesterday";
	public static final String MNEMOSYNE_DENECHAIN_PULSE_COUNT="Mnemosyne Pulse Count";
	
	public static final String MNEMOSYNE_DENECHAIN_CURRENT_WEEK="Mnemosyne Current Week";
	public static final String MNEMOSYNE_DENECHAIN_CURRENT_MONTH="Mnemosyne Current Month";
	public static final String MNEMOSYNE_DENECHAIN_CURRENT_QUARTER="Mnemosyne Current Quarter";
	public static final String MNEMOSYNE_DENECHAIN_CURRENT_SEMESTER="Mnemosyne Current Semester";
	public static final String MNEMOSYNE_DENECHAIN_CURRENT_YEAR="Mnemosyne Current Year";
	public static final String MNEMOSYNE_TIMESTAMP_FORMAT="dd/MM/yyyy HH:mm:ss";
	public static final String MNEMOSYNE_DATE_FORMAT="dd/MM/yyyy";
	public static final String MNEMOSYNE_TIME_FORMAT="HH:mm";
	
	public static final String MNEMOSYNE_LIST_FILE_INFO_OPERATION="Mnemosyne List File Info Operation";
	public static final String MNEMOSYNE_CREATE_DENE_OPERATION="Mnemosyne Create Dene Operation";
	public static final String MNEMOSYNE_COPY_DENE_OPERATION="Mnemosyne Copy Dene Operation";
	public static final String MNEMOSYNE_ADD_DENEWORD_TO_DENE_OPERATION="Mnemosyne Add DeneWord To Dene Operation";
	public static final String MNEMOSYNE_UPDATE_VALUE_OPERATION="Mnemosyne Update Value Operation";

	public static final String MNEMOSYNE_DENEWORD_OPERATION="Operation";
	
	public static final String MNEMOSYNE_DENE_WORD_TYPE_TARGET="Mnemosyne Target";
	public static final String MNEMOSYNE_DENE_WORD_TYPE_COPY_DENEWORD="Copy DeneWord";
	public static final String MNEMOSYNE_DENE_WORD_TYPE_COPY_DENE="Copy Dene";
	
	public static final String MNEMOSYNE_DENE_TYPE_COPY_DENEWORD="Copy Dene";
	
	public static final String MNEMOSYNE_DENE_WORD_TYPE_CREATE_DENEWORD_SOURCE="Create DeneWord Source";
	public static final String MNEMOSYNE_DENEWORD_FILE_LIST_PATH="File List Path";
	public static final String MNEMOSYNE_DENEWORD_NEW_DENE_NAME="New Dene Name";
	public static final String MNEMOSYNE_DENEWORD_TARGET_POSITION="Target Position";
	
	public static final String MNEMOSYNE_CREATE_DENEWORD_ADD_TO_DENE_OPERATION="Create DeneWord Add To Dene";
	public static final String MNEMOSYNE_DENEWORD_TYPE_TRANSFORMATION_DATA_SOURCE="Transformation Data Source";

	public static final String MNEMOSYNE_DENEWORD_TRANSFORMATION_OPERATION="DeneWord Transformation";
	public static final String MNEMOSYNE_DENEWORD_TRANSFORMATION_OPERATION_FUNCTION="DeneWord Transformation Function";
	public static final String MNEMOSYNE_DENEWORD_TRANSFORMATION_OPERATION_FUNCTION_ELAPSED_TIME="Transformation Function Seconds To Elapsed Time";
	
	public static final String MNEMOSYNE_DENEWORD_AGGREGATION_OPERATION="DeneWord Aggregation";
	public static final String MNEMOSYNE_DENEWORD_AGGREGATION_OPERATION_AGGREGATE_TO="Aggregate To";
	public static final String MNEMOSYNE_DENEWORD_AGGREGATION_OPERATION_AGGREGATE_Value="Aggregate Value";
	public static final String MNEMOSYCON_MAXIMUM_PERCENTAGE="Maximum Percentage";

	
	public static final String TIME_UNIT_DAY="Day";
	public static final String TIME_UNIT_WEEK="Week";
	public static final String TIME_UNIT_MONTH="Month";
	public static final String TIME_UNIT_YEAR="Year";
	public static final String PULSE_TABLE="pulse";
	public static final String IP_ADDRESS="IP Address";
	public static final String DEVICE_NAME="Device Name";
	public static final String MAC_ADDRESS="Mac Address";
	public static final String WHITE_LIST_STATUS="White List Status";
	public static final String IS_DEVICE_PRESENT="Is Present";
	public static final String IS_DEVICE_MISSING = "Is Missing";
	public static final String IS_DEVICE_NEW="Is New";
	public static final String NETWORK_DEVICE_ACTIVITY_TABLE="networkdeviceactivity";
	public static final String NETWORK_DEVICE_WHITELIST_TABLE="networkdevicewhitelist";
	public static final String NETWORK_SCAN_MILLIS="Scan Millis";
	public static final String NETWORK_SCAN_TIME_STRING="Scan Time String";
	public static final String DEVICE_LIST="DeviceList";
	public static final String DEVICE_LIST_CHANGES="DeviceListChanges";
	public static final String ORGANISMPULSE_TABLE="organismpulse";
	
	public static final String MOTHER_REMEMBERED_VALUES_TABLE="motherrememberedvalues";
	public static final String REMEMBERED_DENEWORDS_TABLE="remembereddenewords";
	public static final String MUTATION_EVENT_TABLE="mutationevent";
	public static final String COMMAND_REQUESTS_TABLE="commandrequests";
	
	public static final String MNEMOSYCON_RULE_TEAM_PARAMETER="Team Parameter";
	public static final String MNEMOSYCON_TEAM_MEMBER="Mnemosycon Team Member";
	public static final String DENE_MNEMOSYCON_TEAM_DEFINITION="Mnemosycon Team Definition";
	public static final String MNEMOSYCON_RULE_TEAM_PARAMETER_TEAM="Team";
	public static final String MNEMOSYCON_RULE_TEAM_PARAMETER_NOT_TEAM="Not Team";
	public static final String MNEMOSYCON_RULE_TEAM_PARAMETER_ALL="All";
	public static final String DENE_TYPE_MNEMOSYCON_RULE_PROCESSING="Mnemosycon Rule Processing";
	public static final String MNEMOSYCON_RULE_EXECUTION_MILLIS="Mnemosycon Rule Execution Millis";
	public static final String PATHOLOGY_MNEMOSYCON_FAILED="Mnemosycon Failed";
	public static final String DENEWORD_TYPE_MNEMOSYCON_SUCCESS_TASKS_POINTER="Mnemosycon Success Tasks Pointer";
	public static final String DENEWORD_TYPE_MNEMOSYCON_FAILURE_TASKS_POINTER="Mnemosycon Failure Tasks Pointer";
	public static final String DENEWORD_TYPE_MNEMOSYCON_SUCCESS_MNEMOSYNE_OPERATIONS_POINTER="Mnemosycon Success Mnemosyne Operations Pointer";
	public static final String DENEWORD_TYPE_MNEMOSYCON_FAILURE_MNEMOSYNE_OPERATIONS_POINTER="Mnemosycon Failure Mnemosyne Operations Pointer";
	public static final String MNEMOSYNE_DENE_WORD_TYPE_DENE_SOURCE="Dene Copy Dene Source";
	public static final String MNEMOSYNE_HOURLY_MUTATION="Hourly";
	public static final String MNEMOSYNE_DAILY_MUTATION="Daily";
	public static final String MNEMOSYNE_WEEKLY_MUTATION="Weekly";
	public static final String MNEMOSYNE_MONTHLY_MUTATION="Monthly";
	public static final String MNEMOSYNE_YEARLY_MUTATION="Yearly";
	public static final String DENEWORD_HOUR_IN_DAY="Hour in Day";
	public static final String DENEWORD_DAY_IN_WEEK="Day in Week";
	public static final String DENEWORD_DAY_IN_MONTH="Day in Month";
	public static final String DENEWORD_MONTH_IN_YEAR="Month in Year";
	public static final String DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH="Mnemosycon Forget Approach";
	public static final String DENEWORD_TYPE_MNEMOSYCON_EXPRESSION_VARIABLE_DEFINITION = "Mnemosycon Expression Variable Definition";
	public static final String DENEWORD_TYPE_MNEMOSYNE_PROCESSING_FUNCTION="Mnemosyne Function";
	
	public static final String DENEWORD_TYPE_MNEMOSYCON_REMEMBERED_DENEWORD="Remembered DeneWord";
	public static final String DENEWORD_TYPE_MNEMOSYCON_REMEMBERED_DENE="Remembered Dene";
	public static final String DENEWORD_TYPE_MNEMOSYCON_REMEMBERED_DENECHAIN="Remembered DeneChain";
	
	public static final String DENE_TYPE_MNEMOSYCON_DENEWORDS_TO_REMEMBER="DeneWords To Remember";
	
	
	public static final String DENEWORD_TYPE_MNEMOSYNE_SAMPLE_FREQUENCY="Mnemosyne Sample Frequency Seconds";
	public static final String VISUALIZATION_OBJECT_TABLE="Table";

	public static final String VISUALIZATION_OBJECT_LINE_GRAPH="Line Graph";
	public static final String DENE_TYPE_VISUALIZATION_STYLE="Visualization Definition";
	public static final String DENEWORD_TYPE_VISUALIZATION_OBJECT="Visualization Object";
	public static final String DENEWORD_TYPE_VISUALIZATION_PARAMETER="Visualization Parameter";
	public static final String DENEWORD_EXECUTION_POSITION="Execution Position";
	public static final String DENEWORD_PERSIST_ORGANISM_PULSE="Persist Organism Pulse";
	public static final String DENEWORD_PERSIST_PULSE="Persist Pulse";
	//
	//
	
	public static final String PATHOLOGY_CAUSE = "Cause";
	public static final String PATHOLOGY_LOCATION = "Location";
	public static final String PATHOLOGY_DENEWORD_TYPE= "Pathology Dene Word Type";
	public static final String PATHOLOGY_LOCATION_MICROCONTROLLER= "Microcontroller";
	public static final String PATHOLOGY_LOCATION_NETWORK= "Network";
	public static final String PATHOLOGY_LOCATION_MEDULA= "Medula";
	public static final String PATHOLOGY_PULSE_LATE= "Pulse Late";
	public static final String PATHOLOGY_HYPOTHALAMUS_DIED= "Hypothalamus died";
	public static final String PATHOLOGY_HEART_DIED= "Heart died";
	public static final String PATHOLOGY_HEART_PULSE_LATE= "Heart Pulse Late";
	public static final String PATHOLOGY_HEART_CRASHED_HPROF= "Heart Crashed hprof";
	
	public static final String PATHOLOGY_CORRUPT_PULSE_FILE= "Corrupt Pulse File";
	public static final String PATHOLOGY_TOMCAT_PING_LATE= "Tomcat Ping Late";
	public static final String PATHOLOGY_DETAILS_LABEL= "Details";
	
	public static final String PATHOLOGY_MEDULA_FORCED_REBOOT="Medula Forced Reboot";
	public static final String MEDULA_PATHOLOGY_MNEMOSYNE_LOCATION="Medula Pathology Mnemosyne Location";
	
	
	
	public static final String PATHOLOGY_EVENT_TIMESTAMP = "Pathology Event Timestamp";
	public static final String PATHOLOGY_EVENT_MILLISECONDS =  "Pathology Event Milliseconds";
	
	
	//
	// Pathology fault deneword type
	//
	public static final String PATHOLOGY_DATA_NOT_AVAILABLE = "Data Not Available";
	public static final String PATHOLOGY_DATA_OUT_OF_RANGE = "Data Out Of Range";
	public static final String PATHOLOGY_AVAILABLE_MEMORY_BELOW_THRESHOLD = "Available Memory Below Threshold";
	public static final String PATHOLOGY_DISK_SPACE_BELOW_THRESHOLD = "Disk Space Below Threshold";
	public static final String PATHOLOGY_PULSE_DURATION_ABOVE_THRESHOLD = "Pulse Generation Above Threshold";
	public static final String PATHOLOGY_ANALYTICON_SOURCES_LATE = "Analyticon Sources Late";
	
	
	public static final String PATHOLOGY_DATA_STALE="Data Stale";
	public static final String PATHOLOGY_INITIAL_STATE_NETWORK_UNAVAILABLE="Initial State Network Unavailable";
	public static final String PATHOLOGY_NETWORK_STATE_MISTMATCH="Network State Mismatch";
	
	public static final String PATHOLOGY_NETWORK_UNAVAILABLE="Network Unavailable";
	public static final String PATHOLOGY_DENE_EXTERNAL_DATA= "External Data Pathology";
	public static final String PATHOLOGY_DENE_SENSOR_OUT_OF_RANGE= "Sensor Out Of Range Pathology";
	public static final String PATHOLOGY_DENE_MICROCONTROLLER_FAULT= "Microcontroller Fault";
	public static final String PATHOLOGY_DENE_MICROCONTROLLER_COMMUNICATION= "Microcontroller Communication Pathology";
	public static final String PATHOLOGY_DENEWORD_OPERATING_SYSTEM_NETWORK_IDENTITY = "Operating System Network Identity";
	public static final String PATHOLOGY_DENEWORD_DENOME_NETWORK_IDENTITY = "Denome Network Identity";
	
	
	public static final String EXTERNAL_SOURCE_TELEONOME_NAME="External Source Teleonome Name";
	public static final String EXTERNAL_DATA_STATUS_STALE="stale";
	public static final String EXTERNAL_DATA_STATUS="ExternalDataStatus";
	public static final String EXTERNAL_DATA_STATUS_OK="success";
	public static final String DENE_SYSTEM_DATA="System Data";
	public static final String DENE_ALERT_MESSAGES="Alert Messages";
	public static final String DENE_MEMORY_STATUS="Memory Status";
	public static final String DENE_WIFI_INFO="Wifi Info";
	public static final String WLAN0="wlan0";
	public static final String WLAN1="wlan1";
	public static final String ETH0="eth0";
	public static final String DENE_WLAN0="wlan0 Info";
	public static final String DENE_WLAN1="wlan1 Info";
	public static final String DENE_ETH0="eth0 Info";
	public static final String DENEWORD_ACCESS_POINT="Access Point";
	public static final String DENEWORD_IP_ADDRESS="IP Address";
	public static final String LINK_QUALITY="Link Quality";
	public static final String SIGNAL_LEVEL="Signal level";
	//
	// the values to use for the status message in case the application calling 
	// the DenomeManager does not set one
	//
	public static final String STATUS_MESSAGE_USE_CURRENT_PULSE_MILLIS="Status Message Use Current Pulse Millis";
	public static final String STATUS_MESSAGE_USE_CURRENT_PULSE_SECONDS="Status Message Use Current Pulse Seconds";
	public static final String STATUS_MESSAGE_USE_CURRENT_PULSE_MINUTES="Status Message Use Current Pulse Minutes";
	public static final String STATUS_MESSAGE_USE_CURRENT_AND_AVAILABLE_PULSE_SECONDS="Status Message Use Current and Available Pulse Seconds";
	public static final String STATUS_MESSAGE_USE_CURRENT_AVAILABLE_PULSE_NUMBER_ANALYTICONS="Status Message Use Current Available Pulse Number of Analyticons";
	public static final String STATUS_MESSAGE_EXTERNAL_DATA_STALE="External Data Stale";

	//
	// The Egg Denes that need to be removed during the fertilization
	public static final String EGG_VIRTUAL_MICROCONTROLLER="Simple Micro Controller";
	public static final String EGG_VIRTUAL_ACTUATOR="VirtualActuator";
	
	//
	// Sperm structure
	//
	public static final String SPERM="Sperm";
	public static final String SPERM_PURPOSE="Purpose";
	public static final String SPERM_PURPOSE_TYPE="Purpose Type";
	public static final String SPERM_PURPOSE_TYPE_CREATE="Create";
	public static final String SPERM_PURPOSE_TYPE_MUTATE="Mutate";
	
	public static final String SPERM_HYPOTHALAMUS="Hypothalamus";
	public static final String SPERM_HYPOTHALAMUS_HOMEOBOXES="Homeoboxes";
	public static final String SPERM_HYPOTHALAMUS_ACTIONS="Actions";
	public static final String SPERM_HYPOTHALAMUS_MUTATIONS="Mutations";
	public static final String SPERM_DENE_TYPE_CREATE_MUTATION="Create Mutation";
	public static final String SPERM_DENE_TYPE_CREATE_DENE_CHAIN="Create Dene Chain";
	public static final String SPERM_ACTION_DENWORD_UPDATE_VALUE_LIST="DeneWords Update List";

	public static final String SPERM_DENE_TYPE_UPDATE_DENEWORD_VALUE="Update DeneWord";
	public static final String SPERM_ACTION_DENEWORD_EXECUTION_POSITION="Execution Position";
	public static final String SPERM_ACTION_DENEWORD_EXECUTION_POINT="Execution Point";
	public static final String SPERM_ACTION_DENEWORD_EXECUTION_POINT_PRE_HOMEBOX="Pre Homebox Insertion";
	public static final String SPERM_ACTION_DENEWORD_EXECUTION_POINT_POST_HOMEBOX="Post Homebox Insertion";
	public static final String SPERM_ACTION_DENEWORD_DENECHAIN_NAME="DeneChain Name";
	public static final String SPERM_ACTION_DENEWORD_MUTATION_NAME="Mutation Name";
	
	public static final String SPERM_MEDULA="Medula";
	public static final String SPERM_PURPOSE_NAME="Name";
	public static final String SPERM_PURPOSE_DESCRIPTION="Description";
	public static final String SPERM_HOMEOBOX_INDEX="Homeobox Index";
	public static final String SPERM_DENE_TYPE_DENEWORD_CARRIER="DeneWord Carrier";
	public static final String SPERM_DENE_TYPE_DENEWORD_REMOVER="DeneWord Remover";
	
	public static final String SPERM_DENE_TYPE_HOMEOBOX_METADATA="HomeoBox Metadata";
	public static final String SPERM_DATE_FORMAT="ddMMyyyy_HHmm";
	
	public static final String SPERM_HOMEOBOX_DENES="Denes";
	public static final String SPERM_ACTIONS_DENES="Denes";
	
	public static final String DENEWORD_TYPE_HOX_DENE_POINTER="Hox Dene Pointer";
	public static final String DENEWORD_TYPE_SPERM_DENOME_OPERATION_POINTER="Sperm Operation Pointer";
	
	
	public static final String SPERM_HOX_DENE_TARGET="Target";
	public static final String SPERM_ACTION_DENE_TARGET="Target";
	
	public static final String SPERM_HOX_DENE_POINTER="Hox Dene Pointer";
	public static final String SPERM_VALIDATED="Sperm Validated";
	//
	// Variables for Sertoli
	//
	public static final String HOMEOBOX_DEFINITION_TYPE="Homeobox Definition Type";
	public static final String HOMEOBOX_DEFINITION_SENSOR="Sensor";
	public static final String HUMAN_INTERFACE_PANEL = "Human Interface Panel";
	public static final String IN_PANEL_POSITION=  "In Panel Position";
	
	public static final String DENECHAIN_REFERENCE="Reference";
	public static final String DENE_STATUS="Status";
	
	
	//
	// the denetypes to make the human interface
	//
	public static final String DENECHAIN_TYPE_HUMAN_INTERFACE_CONTROL_PARAMETERS="Human Interface Control Parameters";
	public static final String DENECHAIN_TYPE_HUMAN_INTERFACE_WEB_PAGE ="Human Interface Web Page";
	public static final String DENEWORD_TYPE_HUMAN_INTERFACE_WEB_PAGE_INCLUDE_IN_NAVIGATION ="Include in Navigation";
	public static final String DENEWORD_TYPE_HUMAN_INTERFACE_WEB_PAGE_PAGE_TITLE ="Page Title";
	public static final String DENEWORD_HUMAN_INTERFACE_WEB_PAGE_PAGE_POSITION ="Page Position";
	public static final String DENEWORD_TYPE_PANEL_DENECHAIN_POINTER="Panel DeneChain Pointer";
	public static final String DENEWORD_TYPE_PANEL_IN_PAGE_POSITION="Panel In Page Position";
	public static final String DENEWORD_TYPE_PANEL_IN_PANEL_POSITION="Panel In Panel Position";
	public static final String DENEWORD_TYPE_PANEL_VISUALIZATION_STYLE="Panel Visualization Style";
	public static final String DENEWORD_TYPE_PANEL_DATA_SOURCE_POINTER="Panel Data Source Pointer";
	public static final String DENEWORD_TYPE_DISPLAY_TABLE_DENEWORD_POINTER="Display Table DeneWord Pointer";
	public static final String DENEWORD_TYPE_CHART_TIME_SCALE_STRING="Panel Time Scale String";
	public static final String DENEWORD_TYPE_HUMAN_INTERFACE_WEB_PAGE_GLYPH_ICON="Lower Nav Glyphicon";
	//
	// the different styles to present data
	//
	public static final String PANEL_VISUALIZATION_STYLE_NETWORK_SENSOR_DEVICE_STATUS_REPORT="NetworkSensorDeviceStatusReport";
	public static final String PANEL_VISUALIZATION_STYLE_NETWORK_SENSOR_DEVICE_LIST_CHANGES_REPORT="NetworkSensorDeviceListChangesReport";
	
	public static final String PANEL_VISUALIZATION_STYLE_SINGLE_VALUE_PANEL_EXTERNAL_DATA="Single Value Panel External Data";
	public static final String PANEL_VISUALIZATION_STYLE_SINGLE_VALUE_PANEL_COMPLETE_WIDTH_EXTERNAL_DATA="Single Value Panel Complete Width External Data";
	public static final String PANEL_VISUALIZATION_COMPLETE_DENE_STYLE_SINGLE_VALUE_PANEL_EXTERNAL_DATA="Complete Dene Single Value Panel External Data";
	public static final String PANEL_VISUALIZATION_SEARCH_PANEL="Search Panel";
	public static final String PANEL_VISUALIZATION_STYLE_SINGLE_VALUE_PANEL="Single Value Panel";
    public static final String PANEL_VISUALIZATION_STYLE_SINGLE_VALUE_PANEL_COMPLETE_WIDTH="Single Value Panel Complete Width";
	public static final String PANEL_VISUALIZATION_COMPLETE_DENE_STYLE_SINGLE_VALUE_PANEL="Complete Dene Single Value Panel";
	public static final String PANEL_VISUALIZATION_COMPLETE_DENE_CHAIN_STYLE_SINGLE_VALUE_PANEL="Complete Dene Chain Single Value Panel";
	public static final String PANEL_VISUALIZATION_WELL_SINGLE_VALUE_PANEL="Well Single Value";
	public static final String PANEL_TITLE="Panel Title";
	
	public static final String PANEL_VISUALIZATION_STYLE_DENEWORD_TABLE="DeneWord Table";
	public static final String PANEL_VISUALIZATION_STYLE_SHORT_TERM_WEATHER_FORECAST="Short Term Weather Forecast Panel";
	public static final String PANEL_VISUALIZATION_STYLE_DAILY_WEATHER_FORECAST="Daily Weather Forecast Panel";
	public static final String PANEL_VISUALIZATION_STYLE_MANUAL_ACTION_WITH_TIMER="Manual Action With Timer Panel";
	public static final String PANEL_VISUALIZATION_STYLE_SINGLE_VALUE_PANEL_EXTERNAL_TIMESTAMP="Single Value Panel External Timestamp";

	
	
	
	public static final String PANEL_VISUALIZATION_STYLE_PATHOLOGY="Pathology Panel";
	public static final String PANEL_VISUALIZATION_STYLE_LINE_CHART="Line Chart Panel";
	public static final String PANEL_VISUALIZATION_STYLE_BAR_CHART="Bar Chart Panel";
	
	public static final String PANEL_VISUALIZATION_STYLE_CSV_MULTI_LINE_CHART="CSV File MultiLine Chart Panel";
	public static final String PANEL_VISUALIZATION_STYLE_MULTI_LINE_CHART="MultiLine Chart Panel";
	public static final String PANEL_VISUALIZATION_STYLE_PIE_CHART="Pie Chart Panel";
	public static final String PANEL_VISUALIZATION_ORGANISM_VIEW="Organism View Panel";
	public static final String PANEL_VISUALIZATION_STYLE_MNEMOSYNE_TABLE="Mnemosyne Table Panel";
	public static final String PANEL_VISUALIZATION_STYLE_IMAGE="Image Panel";
	public static final String PANEL_VISUALIZATION_STYLE_UPDATE_MULTIPLE_DENEWORDS_FORM="Update Multiple Denewords Form Panel";
	public static final String DENEWORD_TYPE_PANEL_DATA_DISPLAY_NAME="Panel Display Name";
	public static final String DENEWORD_RECORD_TIMESTAMP="Record Timestamp";
	public static final String DENEWORD_TYPE_RESTART_NEEDED="Restart Needed";
	
	public static final String PANEL_VISUALIZATION_STYLE_MNEMOSYCON_EVALUATION_REPORT="Mnemosycon Evaluation Report";
	public static final String PANEL_VISUALIZATION_STYLE_ACTION_EVALUATION_REPORT="Action Evaluation Report";
	public static final String PANEL_VISUALIZATION_STYLE_NETWORK_MODE_SELECTOR="Network Mode Selector";
	public static final String PANEL_VISUALIZATION_STYLE_SETTINGS_INFO="Settings Info";
	public static final String PANEL_VISUALIZATION_STYLE_DIAGNOSTICS_INFO="Diagnostics Info";
	
	//
	// LocalStorage Keys
	//
	public static final String LOCAL_STORAGE_SEARCH_KEY="Search";
	public static final String LOCAL_STORAGE_CURRENT_VIEW_KEY="Current View";
	public static final String PROCESSING_QUEUE_POSITION="Processing Queue Position";
	public static final String DENE_TYPE_HUMAN_INTERFACE_PAGE="Human Interface Page";
	public static final String DENE_TYPE_HUMAN_INTERFACE_COMPONENT="Human Interface Component";
	public static final String DENEWORD_TYPE_HUMAN_INTERFACE_COMPONENT_PROPERTY="Human Interface Component Property";
	public static final String DENEWORD_TYPE_HUMAN_INTERFACE_COMPONENT_TEMPLATE="Human Interface Component Template";
	public static final String DENEWORD_TYPE_HUMAN_INTERFACE_COMPONENT_TEMPLATE_URL="Human Interface Component Template URL";
	public static final String DENEWORD_TYPE_HUMAN_INTERFACE_COMPONENT_STYLES="Human Interface Component Styles";
	public static final String DENEWORD_TYPE_EXTERNAL_DATA_SOURCE_DENE="External Data Source Dene";
	public static final String DENEWORD_TYPE_EXTERNAL_TIMESTAMP_DATA_SOURCE_DENE="External Timestamp Data Source Dene";
	public static final String DENEWORD_TYPE_HUMAN_INTERFACE_COMPONENT_STYLES_URL="Human Interface Component Styles URL";
	public static final String DENEWORD_TYPE_WEB_PAGE_VIEW_DEFINITION_POINTER="Web Page View Definition Pointer";
	public static final String DENEWORD_TYPE_COLUMN_DATA_SOURCE_POINTER="Column Data Source Pointer";
	public static final String DENEWORD_DISPLAY_TABLE_COLUMN_DEFINITION_POINTER="Display Table Column Definition Pointer";
	public static final String DENEWORD_TYPE_COLUMN_IN_TABLE_POSITION="Column In Table Position";
	public static final String DENEWORD_TYPE_COLUMN_HEADER="Column Header";
	public static final String DENEWORD_TYPE_COLUMN_TD_CLASS_INFO="TDClassInfo";
	
	//
	// life cycle events
	//
	public static final String EXTENDED_OPERON_EXECUTION_OPERON_TYPE="Extended Operon Execution Operon Type";
	public static final String EXTENDED_OPERON_EXECUTION_OPERON_NAME="Extended Operon Execution Name";
	public static final String EXTENDED_OPERON_EXECUTION_START_TIME="Extended Operon Execution Start Time";
	public static final String EXTENDED_OPERON_PROGRESS_TIME="Extended Operon Progress Time";
	public static final String EXTENDED_OPERON_PROGRESS_CURRENT_STEP="Extended Operon Progress Current Step";
	public static final String EXTENDED_OPERON_PROGRESS_TOTAL_STEPS="Extended Operon Progress Total Steps";
	public static final String EXTENDED_OPERON_EXECUTION_END_TIME="Extended Operon Execution End Time";
	public static final String EXTENDED_OPERON_EXECUTION_PROGRESS_FILE_NAME = "Extended Operon Execution Progress File Name";
	public static final String LIFE_CYCLE_EVENT_START_EXTENDED_OPERON_EXECUTION="Start Extended Operon Execution";
	public static final String LIFE_CYCLE_EVENT_END_EXTENDED_OPERON_EXECUTION="End Extended Operon Execution";
	public static final String LIFE_CYCLE_EVENT_UPDATE_EXTENDED_OPERON_EXECUTION="Update Extended Operon Execution";
	public static final String  EXTENDED_OPERON_EXECUTION_PROGRESS="ExtendedOperonExecutionProgress.json";
	
	public static final String LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE="Start Synchronous Cycle";
	public static final String LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE="End Synchronous Cycle";
	public static final String LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE="Start Asynchronous Cycle";
	public static final String LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE="End Asynchronous Cycle";
	public static final String LIFE_CYCLE_EVENT_MOTHER_ALERT_WPS="Mother Alert WPS";
	
	public static final String LIFE_CYCLE_EVENT_START_WPS="Start WPS";
	public static final String LIFE_CYCLE_EVENT_END_WPS="End WPS";
	public static final String LIFE_CYCLE_EVENT_START_COMMA="Start Comma";
	public static final String LIFE_CYCLE_EVENT_END_COMMA="End Comma";
	public static final String LIFE_CYCLE_EVENT_START_AWAKE="Start Awake";
	public static final String LIFE_CYCLE_EVENT_END_AWAKE="End Awake";
	
	public static final int LIFE_CYCLE_EVENT_SYNCHRONOUS_VALUE=6;
	public static final int LIFE_CYCLE_EVENT_ASYNCHRONOUS_VALUE=5;
	public static final int LIFE_CYCLE_EVENT_AWAKE_VALUE=4;
	public static final int LIFE_CYCLE_EVENT_ALERT_WPS_VALUE=3;
	public static final int LIFE_CYCLE_EVENT_WPS_VALUE=2;
	public static final int LIFE_CYCLE_EVENT_COMMA_VALUE=1;
	
	
	public static final String WPS_ALERT="WPS Alert";
	
	//
	//
	//
	// everything related to the mnemotycons
	//
	
	//
	// the two different sources for a remembereddeneword, pulse or wps
	public static final String REMEMBERED_DENEWORD_SOURCE_PULSE="Pulse";
	public static final String REMEMBERED_DENEWORD_SOURCE_WPS="WPS";
	
	public static final String DENEWORD_TYPE_MNEMOSYCON_TYPE="Mnemosycon Type";
	public static final String MNEMOSYCON_TYPE_STATIC="Static";
	public static final String MNEMOSYCON_TYPE_DYNAMIC="Dynamic";
	public static final String MNEMOSYCON_TYPE_REMEMBERED_DENEWORD_ANALYSIS="Remembered DeneWord Analysis";
	public static final String DENEWORD_TYPE_MNEMOSYCON_DATABASE_FIELD="Mnemosycon Rule Database Field";
	
	public static final String MNEMOSYCON_DATA_SOURCE_FILE_SYSTEM="File System";
	public static final String MNEMOSYCON_DATA_SOURCE_DATABASE="Database";
	public static final String MNEMOSYCON_DATA_LOCATION_PULSE="Pulse";
	public static final String MNEMOSYCON_DATA_LOCATION_ORGANISM="OrganismPulse";
	public static final String MNEMOSYCON_DATA_LOCATION_REMEMBERED_DENEWORDS="RememberedDeneWords";
	
	public static final String MNEMOSYCON_DATA_LOCATION_COMMAND_REQUESTS="CommandRequests";
	public static final String MNEMOSYCON_DATA_LOCATION_MUTATION_EVENT="MutationEvent";
	public static final String MNEMOSYCON_DELETE_OLDER_THAN="Delete Older Than";
	public static final String MNEMOSYCON_ROWS_DELETED="Rows Deleted";
	
	public static final String DISK_SPACE_BEFORE_MNEMOSYCON_RULE="Disk Space Before Rule";
	public static final String DISK_SPACE_AFTER_MNEMOSYCON_RULE="Disk Space After Rule";
	public static final String MNEMOSYCON_RULE_FILES_DELETED="Files Deleted";
	public static final String MNEMOSYCON_RULE_OLDEST_FILE_DELETED="Oldest File Deleted";
	public static final String MNEMOSYCON_RULE_NEWEST_FILE_DELETED="Newest File Deleted";
	
	public static final String MNEMOSYCON_RULE_SOURCE="Mnemosycon Rule Source";
	public static final String MNEMOSYCON_RULE_LOCATION="Mnemosycon Rule Location";
	public static final String MNEMOSYCON_RULE_FILE_PREFIX="Mnemosycon Rule File Prefix";
	public static final String MNEMOSYCON_RULE_ALL_FILES="*";
			
	//public static final String DENECHAIN_MNEMOSYCON_LOGIC_PROCESSING="Mnemosycon Logic Processing";
	public static final String DENE_TYPE_MNEMOSYCON_PROCESSING="Mnemosycon Processing";
	public static final String DENEWORD_TYPE_MNEMOSYCON_RULES_LIST_POINTER="Mnemosycon Rules List Pointer";
	public static final String DENEWORD_TYPE_MNEMOSYCON_RULE_POINTER="Mnemosycon Rule Pointer";
	public static final String DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_DATABASE_SIZE_TO_DISK_SIZE="Percentage Database To Disk";
	public static final String DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_ORGANISMPULSE_SIZE_TO_DISK_SIZE="Percentage OrganismPulse To Disk";
	public static final String DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_PULSE_SIZE_TO_DISK_SIZE="Percentage Pulse To Disk";
	
	public static final String  DENEWORD_FREE_SPACE_BEFORE_MNEMOSYCON = "Free Space Before Mnemosycon";
	public static final String  DENEWORD_FREE_SPACE_AFTER_MNEMOSYCON = "Free Space After Mnemosycon";
	public static final String  DENEWORD_MNEMOSYCON_EXECUTION_TIME = "Total Execution Duration Milliseconds";
	public static final String  DENEWORD_MNEMOSYCON_RULES_PROCESSED = "Number Rules Processed";
	public static final String  DENEWORD_MNEMOSYCON_NUMBER_RULES = "Number Rules";
	
	
	public static final String MNEMOSYCON_PATHOLOGY_MNEMOSYNE_LOCATION="Mnemosycon Pathology Mnemosyne Location";
	public static final String MNEMOSYCON_ANALYSIS_IDENTITY = "Mnemosycon Analysis Identity";
	public static final String MNEMOSYCON_ANALYSIS_KIND = "Mnemosycon Analysis Kind";
	public static final String MNEMOSYCON_ANALYSIS_PERIOD_TIME_UNIT= "Mnemosycon Analysis Period Time Unit";
	public static final String MNEMOSYCON_ANALYSIS_PERIOD_TIME_UNIT_VALUE= "Mnemosycon Analysis Period Time Unit Value";
	
	public static final String MNEMOSYCON_RULE_TIME_UNIT="Mnemosycon Rule Until Time Unit";
	public static final String MNEMOSYCON_RULE_TIME_UNIT_VALUE="Mnemosycon Rule Until Time Value";
	
	public static final String DENECHAIN_MNEMOSYCONS="Mnemosycons";
	public static final String DENE_TYPE_MNEMOSYCON="Mnemosycon";
	public static final String DENE_TYPE_MNEMOSYCON_DATA_SOURCE="Mnemosycon Data Source";
	
	public static final String DENEWORD_TYPE_MNEMOSYCON_PROFILE_POINTER="Mnemosycon Profile Pointer";
	public static final String MNEMOSYCON_MILLIS_STARTING_POINT="MillisStartingPoint";
	public static final String MNEMOSYCON_NEXT_BATCH_MILLIS_STARTING_POINT="NextBatchMillisStartingPoint";
	public static final String MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH="Number Pulses Per Batch";
	public static final String MNEMOSYCON_ORGANISM_TELEONOME_TO_PUBLISH="Organism Teleonome To Publish";
	public static final String MNEMOSYCON_ORGANISM_TELEONOME_TO_PUBLISH_ALL_TELEONOME="All Teleonomes";
	public static final String MNEMOSYCON_FUNCTION="Mnemosycon Function";
	public static final String PATHOLOGY_SQL_EXCEPTION="SQL Exception";
	public static final String PATHOLOGY_JSON_EXCEPTION="JSON Exception";
	public static final String PATHOLOGY_DENE_MNEMSYCON_PROCESSING_ERROR="Mnemosycon Processing Error";
	public static final String PATHOLOGY_EXCEPTION_STACK_TRACE="Exception Stacktrace";
	public static final String DENECHAIN_MNEMOSYCON_PROCESSING="Mnemosycon Processing";
	
	public static final String DENE_TYPE_MNEMOSYCON_ACTION_PROCESSING="Mnemosycon Action Processing";
	public static final String DENE_NAME_MNEMOSYCON_LEARN_MY_HISTORY="Learn My History";
	public static final String DENE_NAME_MNEMOSYCON_LEARN_OTHER_HISTORY="Learn Others History";
	public static final String DENE_NAME_MNEMOSYCON_LEARN_OTHER_HISTORY_TELEONOME="Learn Others History Teleonome";
	public static final String MNEMOSYCON_NUMBER_OF_SECONDS_TO_COMPLETE_CYCLE="Seconds To Complete Cycle";
	public static final String MNEMOSYCON_CYCLE_START_MILLISECONDS="Cycle Start Milliseconds";
	public static final String MNEMOSYCON_CYCLE_START_TIMESTAMP="Cycle Start Timestamp";
	public static final String MNEMOSYCON_MAXIMUM_NUMBER_OF_RECORDS_PER_BATCH="Maximum Number Records Per Batch";
	public static final String MNEMOSYCON_MINIMUM_NUMBER_OF_RECORDS_PER_BATCH="Minimum Number Records Per Batch";
	public static final String MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH_NEEDED_FOR_TIME_RESTRICTION="Records Needed For Time Restriction";

	//
	// the topics for the heart
	//'
	public static int HEART_QUALITY_OF_SERVICE=1;

	
	public static final String HEART_TOPIC_STATUS="Status";
	public static final String HEART_TOPIC_PULSE_STATUS_INFO="PulseStatusInfo";
	public static final String HEART_TOPIC_PULSE_STATUS_INFO_SECUNDARY="PulseStatusInfoSecundary";
	public static final String HEART_TOPIC_UPDATE_FORM_STATUS="UpdateFormStatus";
	public static final String HEART_TOPIC_UPDATE_FORM_REQUEST="UpdateFormRequest";
	public static final String HEART_TOPIC_UPDATE_FORM_RESPONSE="UpdateFormResponse";
	public static final String HEART_TOPIC_ASYNC_CYCLE_UPDATE="AsyncCycleUpdate";

	public static final String HEART_TOPIC_BLINK="Blink";
	public static final String HEART_TOPIC_ADA_STATUS="AdaStatus";
	public static final String HEART_TOPIC_ORGANISM_STATUS="OrganismStatus";
	public static final String HEART_TOPIC_ORGANISM_UPDATE="OrganismUpdate";
	public static final String HEART_TOPIC_AVAILABLE_SSIDS="Available SSID";
	public static final String HEART_TOPIC_EXECUTE_MANUAL_ACTION="Manual Action";
	public static final String HEART_TOPIC_RESIGNAL="Resignal";
	
	public static final String PROCESS_HYPOTHALAMUS="Hypothalamus";
	public static final String PROCESS_HEART="Heart";
	public static final String PROCESS_WEB_SERVER="Web Server";

	

	

	

	
	
	
	
	
	       
	
	

}
