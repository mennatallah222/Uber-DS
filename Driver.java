import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Driver extends User {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean inRide;
    private Customer assignedCustomer;

    //user-defined review
    private List<Integer> rideComfortRatings;
    private List<Integer> attitudeRatings;
    private List<Integer> cleanlinessRatings;
    private List<Integer> safetyRatings;
    private List<Integer> overallRatings;
    private List<String> comments;

    private double avgRating;

    public Driver(int id, String username, String password, Socket socket) {
        super(id, username, password, "Driver");
        this.socket = socket;
        this.inRide = false;
        
        this.rideComfortRatings = new ArrayList<>();
        this.attitudeRatings = new ArrayList<>();
        this.cleanlinessRatings = new ArrayList<>();
        this.safetyRatings = new ArrayList<>();
        this.overallRatings = new ArrayList<>();
        this.comments = new ArrayList<>();

        this.avgRating = 0.0;

        try {
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public double getAvgRating() {
        return avgRating;
    }

    public void addReview(int rideComfort, int attitude, int cleanliness, int safety, String comment) {
        rideComfortRatings.add(rideComfort);
        attitudeRatings.add(attitude);
        cleanlinessRatings.add(cleanliness);
        safetyRatings.add(safety);
        comments.add(comment);
        double userOverallRating = (rideComfort + attitude + cleanliness + safety) / 4.0;
        int scaledRating = (int) Math.round(userOverallRating); 
        overallRatings.add(scaledRating);
        calculateAvgRating();
    }

    private void calculateAvgRating() {
        int sum = 0;
        for (int rating : overallRatings) {
            sum += rating;
        }
        this.avgRating = overallRatings.isEmpty() ? 0.0 : (double) sum / overallRatings.size();
    }

    public List<String> getReviews() {
        return comments;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isInRide() {
        return inRide;
    }

    public void setInRide(boolean inRide) {
        this.inRide = inRide;
    }

    public void setAssignedCustomer(Customer customer) {
        this.assignedCustomer = customer;
    }

    public Customer getAssignedCustomer() {
        return this.assignedCustomer;
    }
}
