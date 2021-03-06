package com.jarics.trainbot.plan;

import java.util.List;

/**
 * This class contains common methods to create a plan for all sports.
 *
 */

public abstract class PlanBuilder {

    /**
     * These four ratios are used to increment the first three weeks and decrease the fourth one.
     * This is what modulates the "phase" workout. The fourth week is the end of a micro cycle.
     */
    protected double[] weekDecrementRatio = { 0.09, 0.09, 0.09, -0.11 };

    /**
     * Returns the maximum volume you should train for the target distance.
     * This is used when calculating highest distance to train at low intensity.
     * This ratio is used to set end of training distance to a value above the race distance.
     * If you plan on doing a 10k race, you need to sustain longer distance 15km for instance.
     * This is used in the calculation for volume training.
     * @return
     */
    protected abstract double getMaximumVolumeRatio();


    /**
     * Returns the approximate volume training time for each distance in the plan and a given ftp.
     * @param numberOfWeeks
     * @param ftp
     * @param distance
     * @return
     */
    public abstract List<Session> getVolumeSessions(int numberOfWeeks, double ftp, double distance);

    /**
     * Returns the approximate intensity time for each distance in the plan for a given ftp.
     * @param numberOfWeeks
     * @param ftp
     * @param distance
     * @return
     */
    public abstract List<Session> getIntervalsSessions(int numberOfWeeks, double ftp, double distance);

    /**
     * Pace is a min/km or sec/meters etc. All values must be
     * synchronized with Speed and Pace. For instances: for met[i] et calculated with pace[i] and speed[i]
     * @return
     */
    protected abstract int[] getPace();

    /**
     * Speed is km/hr. All values must be
     * synchronized with Speed and Pace. For instances: for met[i] et calculated with pace[i] and speed[i]
     * @return
     */
    protected abstract double[] getSpeed();

    /**
     * MET is sport specific effort based on statistical studies. All values must be
     * synchronized with Speed and Pace. For instances: for met[i] et calculated with pace[i] and speed[i]
     * @return
     */
    protected abstract double[] getMet();

    /**
     * Implementing classes can modulate values using this method.
     * This method is used by getIncrementalDistances().
     */
    protected double gen(double seed) {
        return seed;
    }

    /**
     * Returns weekly increment ratio. This may vary between individuals, sports and if
     * you want to simulate a different variation in the plan (e.i. undertraing etc)
     * @param idx
     * @return
     */
    protected double getWeekIncrementRatio(int idx) {
        return weekDecrementRatio[idx];
    }

    /**
     * Returns a list of incremental distances based on the target race targetDistance and number of weeks to train.
     * It uses micro cycle of 4 weeks.
     * @param numberOfWeeks, number of week before the race.
     * @param targetDistance, target race targetDistance.
     * @return
     */
    private double[] getIncrementalDistances(int numberOfWeeks, double targetDistance) {
        double[] incrementalDistances = new double[numberOfWeeks];
        incrementalDistances[numberOfWeeks - 1] = gen(targetDistance);
        for (int i = numberOfWeeks - 2; i > -1; i--) {
            double volume = incrementalDistances[i + 1] - getWeekIncrementRatio((i + 1) % 4) * incrementalDistances[i + 1];
            incrementalDistances[i] = volume;
        }
        return incrementalDistances;
    }

    /**
     * Returns the closest corresponding MET value for given an FTP.
     * @param ftp, your last ftp tested value
     * @return
     */
    protected double getMet(double ftp) {
        int idx = getIdx(ftp);
        return getMet()[idx];
    }

    /**
     * Since MET values are obtained by statistical studies and are not available for all
     * possible ftp values, we return the closest values using nearest value.
     * @param ftp
     * @return
     */
    private int getIdx(double ftp) {
        double myNumber = ftp;
        double distance = Math.abs(getPace()[0] - myNumber);
        int idx = 0;
        for (int c = 1; c < getPace().length; c++) {
            double cdistance = Math.abs(getPace()[c] - myNumber);
            if (cdistance < distance) {
                idx = c;
                distance = cdistance;
            }
        }
        return idx;
    }

    /**
     * Returns the closest corresponding MET value for given an FTP.
     * @param ftp
     * @return
     */
    protected double getSpeed(double ftp) {
        int idx = getIdx(ftp);
        return getSpeed()[idx];
    }

    /**
     * Return the target (maximum) distance for training based on race distance.
     * @param raceDistance
     * @return
     */
    private double getTargetVolume(double raceDistance) {
        return getMaximumVolumeRatio() * raceDistance + raceDistance;
    }

    /**
     * This will return the intensity plan for each week.
     * We modulate the distance using you MET score. The better is you MET
     * the farther your capable to train.
     * @param raceDistance
     * @param ftp
     * @param numberOfWeeks
     * @return
     */
    public double[] getIntervalsDistances(double raceDistance, double ftp, int numberOfWeeks) {
        double distance = raceDistance + raceDistance * getMet(ftp) / 100;
        return getIncrementalDistances(numberOfWeeks, distance);
    }

    /**
     * This will return the intensity plan for each week.
     * We modulate the distance using you MET score. The better is you MET
     * the farther your capable to train.
     * @param raceDistance
     * @param ftp
     * @param numberOfWeeks
     * @return
     */
    public double[] getVolumeDistances(double raceDistance, double ftp, int numberOfWeeks) {
        double volumeTargetDistance = getTargetVolume(raceDistance);
        double distance = volumeTargetDistance + volumeTargetDistance * getMet(ftp) / 100;
        return getIncrementalDistances(numberOfWeeks, distance);
    }

}
