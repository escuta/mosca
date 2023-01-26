/*

  Reads $GPGGA NMEA messages (or $GNGGA as fallback) in RTK solution on TCP server output of rtkrcv or rtknavi_qt and extracts precision GPS coordinates. Requires prior launch of a virtual TTY pair as well as the TCP server being active. This utility writes coords in Mosca's format to one member of the TTY pair so that Mosca can read it by connecting to the other. TTY pair established with:

  sudo socat -d -d pty,link=/dev/ttyVA00,echo=0,perm=0777 pty,link=/dev/ttyVB00,echo=0,perm=0777

  C code derived from:

  https://stackoverflow.com/questions/19868156/parsing-code-for-gps-nmea-string

  https://www.geeksforgeeks.org/tcp-server-client-implementation-in-c/

*/

#include <arpa/inet.h> // inet_addr()
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h> // read(), write(), close()
#include <fcntl.h> // Contains file controls like O_RDWR
#include <errno.h> // Error integer and strerror() function
#include <termios.h> // Contains POSIX terminal control definitions
#include <stdbool.h>

#define MAX 150
#define SA struct sockaddr

#define GNSS_HEADER_LENGTH 5
#define GNSS_PACKET_START '$'
#define GNSS_TOKEN_SEPARATOR ','

#define FALSE 0
#define TRUE 1

// SETTINGS

#define VERBOSE    // Print text to terminal
//#define ALTITUDE // include altitude in transmission
#define SERPORT "/dev/ttyVA00"
#define TCPADDRESS "127.0.0.1"
#define TCPPORT 1234
#define BAUDRATE B115200
#define NMEA "GPGGA" // precision data 
#define NMEAFALLBACK "GNGGA" // comment out to restrict to NMEA setting above
// note, if either NMEA headers are changed from GxGGA,
// will likely need to select correct data fields for serial message below

typedef struct 
{
  //  double lat;
  //  double lon;
  char  lat[13];
  char  lon[13];
#ifdef ALTITUDE
  char  alt[13];
#endif
} message_t;

message_t message;


//To trim a string contains \r\n
void str_trim(char *str){
  while(*str){
    if(*str == '\r' || *str == '\n'){
      *str = '\0';
    }
    str++;
  }
}

bool parse_gnss_token(char *data_ptr, char *header, int repeat_index, int token_index, char *result) {
  bool gnss_parsed_result = FALSE; // To check GNSS data parsing is success
  bool on_header = FALSE;
  // For header
  int header_repeat_counter = 0;
  int header_char_index = 0; // each char in header index
  // For counting comma
  int counted_token_index = 0;
  //  To hold the result character index
  bool data_found = FALSE;
  char *result_start = result;
  char header_found[10];
  while (*data_ptr) {
    // 1. Packet start
    if (*data_ptr == GNSS_PACKET_START) {
      on_header = TRUE;
      header_char_index = 0; // to index each character in header
      data_found = FALSE; // is data part found
      data_ptr++;
    }
    // 2. For header parsing
    if (on_header) {
      if (*data_ptr == GNSS_TOKEN_SEPARATOR || header_char_index >= GNSS_HEADER_LENGTH) {
	on_header = FALSE;
      } else {
	header_found[header_char_index] = *data_ptr;
	if (header_char_index == GNSS_HEADER_LENGTH - 1) { // Now Header found
	  header_found[header_char_index + 1] = '\0';
	  on_header = FALSE;
	  if (!strcmp(header, header_found)) {
	    // Some headers may repeat - to identify it set the repeat index
	    if (header_repeat_counter == repeat_index) {
	      //printf("Header: %s\r\n", header_found );
	      data_found = TRUE;
	    }
	    header_repeat_counter++;
	  }
	}
	header_char_index++;
      }
            
    }
    // 3. data found
    if (data_found) {
      // To get the index data separated by comma
      if (counted_token_index == token_index && *data_ptr != GNSS_TOKEN_SEPARATOR) {
	// the data to parse
	*result++ = *data_ptr;
	gnss_parsed_result = TRUE;
      }
      if (*data_ptr == GNSS_TOKEN_SEPARATOR) { // if ,
	counted_token_index++; // The comma counter for index
      }
      // Break if the counted_token_index(token_counter) greater than token_index(search_token)
      if (counted_token_index > token_index) {
	break;
      }

    }
    // Appending \0 to the end
    *result = '\0';
    // To trim the data if ends with \r or \n
    str_trim(result_start);
    // Input data
    data_ptr++;
  }
  return gnss_parsed_result;
}

double GpsEncodingToDegrees( char* gpsencoding )
{
  double a = strtod( gpsencoding, 0 ) ;
  double d = (int)a / 100 ;
  a -= d * 100 ;
  return d + (a / 60) ;
}

void process(int sockfd, int serial_port)
{
  char buff[MAX];
  int n; 
  char res[100];
  char *ptr;
  char latitude[20];
  char longitude[20];
  uint8_t head[] = { 251, 252, 253, 254 };
  uint8_t tail[] = { 255 };
  char nmeaMessage[6];
  bool latNeg = false;
  bool lonNeg = false;
  for (;;) {
    memset(buff, 0, sizeof(buff));
    read(sockfd, buff, sizeof(buff));
    //printf(buff);
    //printf("\n\n");
    if ((strncmp(buff, "exit", 4)) == 0) {
#ifdef VERBOSE
      printf("Client Exit...\n");
#endif
      break;
    }
#ifdef NMEAFALLBACK
    if (strstr(buff, NMEA) != NULL) {
      strncpy(nmeaMessage, NMEA, 5);
    } {
      strncpy(nmeaMessage, NMEAFALLBACK, 5);
    }
#else
    strncpy(nmeaMessage, NMEA, 5);
#endif





#ifdef NMEAFALLBACK
    if ( (strstr(buff, NMEAFALLBACK) != NULL) ||
	 (strstr(buff, NMEA) != NULL) ) {
#else
    if (strstr(buff, NMEA) != NULL) {
#endif
#ifdef ALTITUDE	    
	for(int i=1;i<=9;i++) {
#else
	  for(int i=1;i<=5;i++) {
#endif
	    parse_gnss_token(buff, nmeaMessage, 0, i, res);
	    if(i == 2) {
	      double latInDegrees = GpsEncodingToDegrees(res);
	      if(latNeg){latInDegrees = latInDegrees * -1; };
	      sprintf(message.lat, "%.8f", latInDegrees);
	    }
	    if (i == 3) {
	      if (strstr(buff, "S") != NULL) { latNeg = true;}
	    }
	    if (i == 4) {
	      double lonInDegrees = GpsEncodingToDegrees(res);
	      if(latNeg){lonInDegrees = lonInDegrees * -1; };
	      sprintf(message.lon, "%.8f", lonInDegrees);
		
#ifdef VERBOSE
	      printf("%s Lat: %s Lon: %s\n", nmeaMessage, message.lat, message.lon);
#endif
		
	    }
	    if (i == 5) {
	      if (strstr(buff, "W") != NULL) { latNeg = true;}
	    }

#ifdef ALTITUDE
	    if (i == 9) {
	      sprintf(message.alt, "%s", res);
	      //message.alt = (int32_t) strtof(res, &ptr);
	      write(serial_port, head, sizeof(head));
	      write( serial_port, (uint8_t *) &message, sizeof(message) );
	      write(serial_port, tail, sizeof(tail));

#ifdef VERBOSE
	      //printf("%s alt = %08.7f\n", NMEAFALLBACK, message.alt);
	      printf("%s alt = %s\n", nmeaMessage, message.alt);
	      //printf("%s alt (string): %s\r\n", NMEAFALLBACK, res);
#endif
	    }
#endif

	    //sprintf(latitude, "%g", message.lon);
	    //printf("String = %s\n", latitude);
	      
	    //#ifndef VERBOSE
	    //#endif
	  }
	  write(serial_port, head, sizeof(head));
	  write( serial_port, (uint8_t *) &message, sizeof(message) );
	  write(serial_port, tail, sizeof(tail));
	  
	  //	  printf("test\n");
	}


      }
  
  }
 
  int main()
  {
    int sockfd, connfd;
    struct sockaddr_in servaddr, cli;
    int serial_port = open(SERPORT, O_RDWR);
    // Create new termios struct, we call it 'tty' for convention
    struct termios tty;
    // Read in existing settings, and handle any error
    if(tcgetattr(serial_port, &tty) != 0) {
      printf("Error %i from tcgetattr: %s\n", errno, strerror(errno));
      return 1;
    }
    tty.c_cflag &= ~PARENB; // Clear parity bit, disabling parity (most common)
    tty.c_cflag &= ~CSTOPB; // Clear stop field, only one stop bit used in communication (most common)
    tty.c_cflag &= ~CSIZE; // Clear all bits that set the data size 
    tty.c_cflag |= CS8; // 8 bits per byte (most common)
    tty.c_cflag &= ~CRTSCTS; // Disable RTS/CTS hardware flow control (most common)
    tty.c_cflag |= CREAD | CLOCAL; // Turn on READ & ignore ctrl lines (CLOCAL = 1)
    
    tty.c_lflag &= ~ICANON;
    tty.c_lflag &= ~ECHO; // Disable echo
    tty.c_lflag &= ~ECHOE; // Disable erasure
    tty.c_lflag &= ~ECHONL; // Disable new-line echo
    tty.c_lflag &= ~ISIG; // Disable interpretation of INTR, QUIT and SUSP
    tty.c_iflag &= ~(IXON | IXOFF | IXANY); // Turn off s/w flow ctrl
    tty.c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL); // Disable any special handling of received bytes
    
    tty.c_oflag &= ~OPOST; // Prevent special interpretation of output bytes (e.g. newline chars)
    tty.c_oflag &= ~ONLCR; // Prevent conversion of newline to carriage return/line feed
    tty.c_cc[VTIME] = 10;    // Wait for up to 1s (10 deciseconds), returning as soon as any data is received.
    tty.c_cc[VMIN] = 0;
    
    // Set in/out baud rate to be 9600
    //cfsetispeed(&tty, B9600);
    cfsetospeed(&tty, BAUDRATE);

    // Save tty settings, also checking for error
    if (tcsetattr(serial_port, TCSANOW, &tty) != 0) {
      printf("Error %i from tcsetattr: %s\n", errno, strerror(errno));
      return 1;
    }
 
    // Check for errors - redundant?
    if (serial_port < 0) {
      printf("Error %i from open: %s\n", errno, strerror(errno));
    }
    
    // socket create and verification
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd == -1) {
#ifdef VERBOSE
      printf("socket creation failed...\n");
#endif
      exit(0);
    }
    else
#ifdef VERBOSE
      printf("Socket successfully created..\n");
#endif
    memset(&servaddr, 0, sizeof(servaddr));
    //    bzero(&servaddr, sizeof(servaddr));
 
    // assign IP, TCPPORT
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = inet_addr(TCPADDRESS);
    servaddr.sin_port = htons(TCPPORT);
 
    // connect the client socket to server socket
    if (connect(sockfd, (SA*)&servaddr, sizeof(servaddr))
        != 0) {
#ifdef VERBOSE
      printf("connection with the server failed...\n");
#endif
      exit(0);
    }
    else
#ifdef VERBOSE
      printf("connected to the server..\n");
#endif 
    // process data
    process(sockfd, serial_port);
 
    // close the socket and serial port
    close(sockfd);
    close(serial_port);
  }
