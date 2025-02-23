package ai.aletyx.kie.workshop.foureyes;

public class TravelRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String nationality;
    private String countryDestination;

    public TravelRequest() {
    }

    public TravelRequest(String firstName, String lastName, String email, String nationality,
            String countryDestination) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.nationality = nationality;
        this.countryDestination = countryDestination;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getCountryDestination() {
        return countryDestination;
    }

    public void setCountryDestination(String countryDestination) {
        this.countryDestination = countryDestination;
    }

}
