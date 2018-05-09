package com.jarics.trainbot;

import com.jarics.trainbot.entities.AthleteFTP;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.Charset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PersonController Tester.
 *
 * @author <Erick Audet>
 * @version 1.0
 * @since <pre>Feb 5, 2018</pre>
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class StampControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));


    @Before
    public void before() {

    }

    @After
    public void after() {
    }

    @Test
    public void testCreatePlan() throws Exception {
        AthleteFTP wAthleteFTP = new AthleteFTP();
        wAthleteFTP.setFtp(340);
        wAthleteFTP.setTarget(2);

        System.out.println(TestUtil.convertObjectToJsonBytes(wAthleteFTP));
        mockMvc.perform(post("/api/plan").contentType(contentType).content(TestUtil.convertObjectToJsonBytes(wAthleteFTP))).andExpect(status().isOk());

    }

}
