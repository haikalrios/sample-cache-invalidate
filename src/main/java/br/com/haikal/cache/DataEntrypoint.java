package br.com.haikal.cache;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/data")
public class DataEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataEntrypoint.class);

    @Inject
    private DataService dataService;

    @GET
    @Path("/get/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response putKey(@PathParam("key") String key) {
        LOGGER.info("Start put flow by rest. [key:{}]", key);
        return Response.ok(dataService.getData(key)).build();
    }

    @GET
    @Path("/update/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteKey(@PathParam("key") String key) {
        LOGGER.info("Start delete flow by rest. [key:{}]", key);
        dataService.updateData(key);
        return Response.ok("The update is ready and Cache invalidated notification was sent").build();
    }

}
