package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.JdbcUtils;
import org.example.domain.Participant;
import org.example.domain.Trial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class ParticipantDBRepository implements ParticipantRepository<Participant, Integer> {
    private JdbcUtils dbUtils;
    private TrialDBRepository trRepo;
    private static final Logger logger = LogManager.getLogger();

    public ParticipantDBRepository(Properties props, TrialDBRepository trRepo) {
        logger.info("Initializing ParticipantDBRepository with properties: {} ", props);
        dbUtils = new JdbcUtils(props);
        this.trRepo = trRepo;
    }

    @Override
    public void addParticipant(Participant elem) {
        logger.traceEntry("saving task {} ", elem);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("insert into Participant (id, name, age) values (?,?,?)")) {
            preStmt.setInt(1, elem.getId());
            preStmt.setString(2, elem.getName());
            preStmt.setInt(3, elem.getAge());
            int result = preStmt.executeUpdate();
            logger.trace("Saved {} instances", result);
            for (Trial trial : elem.getTrials()) {
                addParticipantToTrial(elem.getId(), trial.getId());
            }
        } catch (SQLException ex) {
            logger.error(ex);
            System.err.println("Error DB " + ex);
        }

        logger.traceExit();
    }

    private void addParticipantToTrial(int participantId, int trialId) {
        logger.traceEntry("adding participant {} to trial {}", participantId, trialId);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("INSERT INTO Participant_Trial (id_participant, id_trial) VALUES (?, ?)")) {
            preStmt.setInt(1, participantId);
            preStmt.setInt(2, trialId);
            int result = preStmt.executeUpdate();
            logger.trace("Saved {} instances", result);
        } catch (SQLException ex) {
            logger.error(ex);
            System.err.println("Error DB " + ex);
        }
        logger.traceExit();
    }


    @Override
    public void deleteParticipant(Participant elem) {

    }

    @Override
    public void updateParticipant(Participant elem, Integer integer) {

    }

    @Override
    public Participant findByIdParticipant(Integer integer) {
        logger.traceEntry("finding task with id {} ", integer);
        Connection con = dbUtils.getConnection();
        Participant participant = null;
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM Participant WHERE id=?")) {
            preStmt.setInt(1, integer);
            try (ResultSet result = preStmt.executeQuery()) {
                if (result.next()) {
                    String name = result.getString("name");
                    int age = result.getInt("age");
                    participant = new Participant(integer, name, age, findParticipantTrialsById(integer));
                }
            }
        } catch (SQLException e) {
            logger.error(e);
            System.err.println("Error DB " + e);
        }
        logger.traceExit(participant);
        return participant;
    }

    private List<Trial> findParticipantTrialsById(Integer integer) {
        logger.traceEntry("finding task with id {} ", integer);
        Connection con = dbUtils.getConnection();
        List<Trial> trials = new ArrayList<Trial>();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM Participant_Trial WHERE id_participant=?")) {
            preStmt.setInt(1, integer);
            try (ResultSet result = preStmt.executeQuery()) {
                if (result.next()) {
                    int id_trial = result.getInt("id_trial");
                    trials.add(trRepo.findByIdTrial(id_trial));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.traceExit(trials);
        return trials;


    }

    @Override
    public List<Participant> findAllParticipant() {
        logger.traceEntry();
        Connection con = dbUtils.getConnection();
        List<Participant> participants = new ArrayList<>();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM Participant")) {
            try (ResultSet result = preStmt.executeQuery()) {
                while (result.next()) {
                    int id = result.getInt("id");
                    String name = result.getString("name");
                    int age = result.getInt("age");
                    Participant participant = new Participant(id, name, age, findParticipantTrialsById(id));
                    participants.add(participant);
                }
            }
        } catch (SQLException e) {
            logger.error(e);
            System.err.println("Error DB " + e);
        }
        logger.traceExit(participants);
        return participants;
    }

}