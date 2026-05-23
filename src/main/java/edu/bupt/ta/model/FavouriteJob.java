package edu.bupt.ta.model;

import edu.bupt.ta.repository.Identifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FavouriteJob implements Identifiable<String> {

    private String applicantId;
    private List<String> favouriteJobIds = new ArrayList<>();
    private LocalDateTime lastUpdated;

    public FavouriteJob() {
    }

    public FavouriteJob(String applicantId, List<String> favouriteJobIds) {
        this.applicantId = applicantId;
        this.favouriteJobIds = favouriteJobIds;
        this.lastUpdated = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return applicantId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public List<String> getFavouriteJobIds() {
        return favouriteJobIds;
    }

    public void setFavouriteJobIds(List<String> favouriteJobIds) {
        this.favouriteJobIds = favouriteJobIds;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isFavourite(String jobId) {
        return favouriteJobIds != null && favouriteJobIds.contains(jobId);
    }

    public void toggleFavourite(String jobId) {
        if (favouriteJobIds == null) {
            favouriteJobIds = new ArrayList<>();
        }
        if (favouriteJobIds.contains(jobId)) {
            favouriteJobIds.remove(jobId);
        } else {
            favouriteJobIds.add(jobId);
        }
        this.lastUpdated = LocalDateTime.now();
    }
}
