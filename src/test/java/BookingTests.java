import Entities.Booking;
import Entities.BookingDates;
import Entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;

public class BookingTests {
    public static Faker faker;
    private static RequestSpecification request;
    private static Booking booking;
    private static BookingDates bookingDates;
    private static User user;

    @BeforeAll
    public static void Setup(){
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();
        user = new User(faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8,10),
                faker.phoneNumber().toString());

        bookingDates = new BookingDates("2018-01-02", "2018-01-03");
        booking = new Booking(user.getFirstName(), user.getLastName(),
                (float)faker.number().randomDouble(2, 50, 100000),
                true,bookingDates,
                "");
        RestAssured.filters(new RequestLoggingFilter(),new ResponseLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest(){
        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .auth().basic("admin", "password123");
    }

    @Test
    public void getAllBookingsById_returnOk(){
            Response response = request
                                    .when()
                                        .get("/booking")
                                    .then()
                                        .extract()
                                        .response();


        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void  getAllBookingsByUserFirstName_BookingExists_returnOk(){
                    request
                        .when()
                            .queryParam("firstName", "Carol")
                            .get("/booking")
                        .then()
                            .assertThat()
                            .statusCode(200)
                            .contentType(ContentType.JSON)
                        .and()
                        .body("results", hasSize(greaterThan(0)));

    }

    @Test
    public void  CreateBooking_WithValidData_returnOk(){

        Booking test = booking;
        given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                    .contentType(ContentType.JSON)
                        .when()
                        .body(booking)
                        .post("/booking")
                        .then()
                        .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
                        .and()
                        .assertThat()
                        .statusCode(200)
                        .contentType(ContentType.JSON).and().time(lessThan(2000L));

    }

    @Test
    public void full_Update_Booking_With_returnOk(){

        Response response = given()
                .contentType(ContentType.JSON)
                .accept("application/json")
                .auth().preemptive().basic("admin", "password123")
                .body(booking)
                .when().put("booking/3")
                .then()
                .statusCode(200)
                .extract().response();

        Assertions.assertNotNull(response.path("firstname"));
        Assertions.assertNotNull(response.path("lastname"));
        Assertions.assertNotNull(response.path("totalprice"));
        Assertions.assertNotNull(response.path("bookingdates"));
        Assertions.assertNotNull(response.path("additionalneeds"));

    }

    @Test
    public void parcial_Update_Booking_With_returnOk(){

        Response response = given()
                .contentType(ContentType.JSON)
                .accept("application/json")
                .auth().preemptive().basic("admin", "password123")
                .body("{\n" +
                        "    \"firstname\" : \"Juliana\",\n" +
                        "    \"lastname\" : \"Serral\"\n" +
                        "}")
                .when().patch("booking/3")
                .then()
                .statusCode(200)
                .extract().response();

        Assertions.assertEquals("Juliana", response.path("firstname"));
        Assertions.assertEquals("Serral", response.path("lastname"));

    }

    @Test
    public void delete_Booking_With_returnOk(){

        // primeiro estou criando o book para após isso conseguir deletar
        Response response = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .when()
                .body(booking)
                .post("/booking")
                .then()
                .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
                .and()
                .assertThat()
                .statusCode(200).extract().response();

        // recuperando o id do book criado anteriormente
        String id = response.path("bookingid").toString();

        given()
                .contentType(ContentType.JSON)
                .accept("application/json")
                .auth().preemptive().basic("admin", "password123")
                .when().delete("booking/"+id)
                .then()
                .statusCode(201);
    }

    @Test
    public void get_Health_Check_returnOk(){
        Response response = request
                .when()
                .get("/ping")
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(201, response.statusCode());
    }


}
