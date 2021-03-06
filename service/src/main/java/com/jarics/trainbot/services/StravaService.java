package com.jarics.trainbot.services;

import com.jarics.trainbot.entities.AthleteActivity;
import com.jarics.trainbot.entities.AthleteFTP;
import com.jarics.trainbot.entities.AthletesFeatures;
import com.jarics.trainbot.entities.BotActivityType;
import com.jarics.trainbot.services.learning.FeatureExtractor;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.Configuration;
import io.swagger.client.api.ActivitiesApi;
import io.swagger.client.auth.OAuth;
import io.swagger.client.model.ActivityType;
import io.swagger.client.model.DetailedActivity;
import io.swagger.client.model.DetailedSegmentEffort;
import io.swagger.client.model.SummaryActivity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class StravaService implements TrainingLogger {

    @Value("${client.id}") private String clientId;

    @Value("${client.secret}") private String clientSecret;

    public List<AthleteActivity> getAthleteActivities(AthleteFTP pAthleteFTP, int pElapseDays) {
        prepareDefaultClient();
        List<AthleteActivity> wAthleteActivities = new ArrayList<>();
        ActivitiesApi apiInstance = new ActivitiesApi();
        //refresh the access token

        apiInstance
          .getApiClient()
          .setAccessToken(pAthleteFTP.getAccessToken());
        LocalDate today = LocalDate.now();
        LocalDate elapseDays = today.minus(pElapseDays, ChronoUnit.DAYS);
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/Oslo");
        Integer before = new Long(today
          .atStartOfDay(zoneId)
          .toEpochSecond()).intValue();
        Integer after = new Long(elapseDays
          .atStartOfDay(zoneId)
          .toEpochSecond()).intValue();
        Integer page = null;
        Integer perPage = null;
        try {
            List<SummaryActivity> result = apiInstance.getLoggedInAthleteActivities(before, after, page, perPage);
            for (SummaryActivity wSummaryActivity : result) {
                DetailedActivity wDetailedActivity = apiInstance.getActivityById(wSummaryActivity.getId(), Boolean.TRUE);
                AthleteActivity wAthleteActivity = convert(wSummaryActivity);
                if (wSummaryActivity
                  .getType()
                  .equals(ActivityType.RIDE) && wDetailedActivity != null) {
                    wAthleteActivity.setWeigthedAvgWatts(calculateWeightedAvgWatts(wDetailedActivity.getSegmentEfforts()));
                }
                if (wAthleteActivity.getType() != null) wAthleteActivities.add(wAthleteActivity);
            }
            return wAthleteActivities;
        } catch (ApiException e) {
            System.err.println("Exception when calling ActivitiesApi#getLoggedInAthleteActivities");
            e.printStackTrace();
            return null;
        }
    }

    private double calculateWeightedAvgWatts(List<DetailedSegmentEffort> segmentEfforts) {
        Float wAvgWatts = 0f;
        for (DetailedSegmentEffort wSegment : segmentEfforts) {
            wAvgWatts = wAvgWatts + wSegment.getAverageWatts();
        }
        return wAvgWatts / segmentEfforts.size();
    }

    private void prepareDefaultClient() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        OAuth strava_oauth = (OAuth) defaultClient.getAuthentication("strava_oauth");
        strava_oauth.setAccessToken(clientId);
    }

    private AthleteActivity convert(SummaryActivity pSummaryActivity) {
        AthleteActivity wAthleteActivity = new AthleteActivity();
        wAthleteActivity.setDistance(pSummaryActivity.getDistance());
        wAthleteActivity.setElapsedTime(pSummaryActivity.getElapsedTime());
        wAthleteActivity.setMovingTime(pSummaryActivity.getMovingTime());
        switch (pSummaryActivity.getType()) {

        case SWIM:
            wAthleteActivity.setType(BotActivityType.SWIM);
            break; // optional

        case RIDE:
            wAthleteActivity.setType(BotActivityType.BIKE);
            break; // optional

        case VIRTUALRIDE:
            wAthleteActivity.setType(BotActivityType.BIKE);
            break; // optional

        case RUN:
            wAthleteActivity.setType(BotActivityType.RUN);
            break; // optional

        case WALK:
            wAthleteActivity.setType(BotActivityType.RUN);
            break; // optional
        }
        return wAthleteActivity;
    }

    @Override
    public AthletesFeatures extractAthletesFeatures(AthleteFTP athleteFTP) {
        FeatureExtractor featureExtractor = new FeatureExtractor();
        return featureExtractor.extract(getAthleteActivities(athleteFTP, 45), athleteFTP.getSwimFtp(), athleteFTP.getBikeFtp(), athleteFTP.getRunFtp());
    }

    public String getAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("code", code);
        map.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("https://www.strava.com/oauth/token", request, String.class);
        return response.getBody();
    }

}
