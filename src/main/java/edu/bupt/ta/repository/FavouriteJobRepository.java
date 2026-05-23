package edu.bupt.ta.repository;

import edu.bupt.ta.model.FavouriteJob;

import java.nio.file.Path;
import java.util.Optional;

public class FavouriteJobRepository extends AbstractJsonRepository<FavouriteJob, String> {

    public FavouriteJobRepository(Path filePath) {
        super(filePath, FavouriteJob.class);
    }

    public Optional<FavouriteJob> findByApplicantId(String applicantId) {
        return findAll().stream()
                .filter(f -> applicantId.equals(f.getApplicantId()))
                .findFirst();
    }

    public void saveForApplicant(FavouriteJob favourite) {
        save(favourite);
    }
}
