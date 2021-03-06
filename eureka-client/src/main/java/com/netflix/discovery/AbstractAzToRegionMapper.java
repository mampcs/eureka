package com.netflix.discovery;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.netflix.discovery.DefaultEurekaClientConfig.DEFAULT_ZONE;

/**
 * @author Nitesh Kant
 */
public abstract class AbstractAzToRegionMapper implements AzToRegionMapper {

    protected static final Logger logger = LoggerFactory.getLogger(InstanceRegionChecker.class);

    /**
     * A default for the mapping that we know of, if a remote region is configured to be fetched but does not have
     * any availability zone mapping, we will use these defaults. OTOH, if the remote region has any mapping defaults
     * will not be used.
     */
    private final Multimap<String, String> defaultRegionVsAzMap =
            Multimaps.newListMultimap(new HashMap<String, Collection<String>>(), new Supplier<List<String>>() {
                @Override
                public List<String> get() {
                    return new ArrayList<String>();
                }
            });

    private final Map<String, String> availabilityZoneVsRegion = new ConcurrentHashMap<String, String>();

    public AbstractAzToRegionMapper() {
        populateDefaultAZToRegionMap();
    }

    @Override
    public void setRegionsToFetch(String[] regionsToFetch) {
        if (null != regionsToFetch) {
            logger.info("Fetching availability zone to region mapping for regions {}", Arrays.toString(regionsToFetch));
            availabilityZoneVsRegion.clear();
            for (String remoteRegion : regionsToFetch) {
                Set<String> availabilityZones = getZonesForARegion(remoteRegion);
                if (null == availabilityZones
                    || (availabilityZones.size() == 1 && availabilityZones.iterator().next().equals(DEFAULT_ZONE))
                    || availabilityZones.isEmpty()) {
                    logger.info("No availability zone information available for remote region: " + remoteRegion +
                                ". Now checking in the default mapping.");
                    if (defaultRegionVsAzMap.containsKey(remoteRegion)) {
                        Collection<String> defaultAvailabilityZones = defaultRegionVsAzMap.get(remoteRegion);
                        for (String defaultAvailabilityZone : defaultAvailabilityZones) {
                            availabilityZoneVsRegion.put(defaultAvailabilityZone, remoteRegion);
                        }
                    } else {
                        String msg = "No availability zone information available for remote region: " + remoteRegion +
                                     ". This is required if registry information for this region is configured to be fetched.";
                        logger.error(msg);
                        throw new RuntimeException(msg);
                    }
                } else {
                    for (String availabilityZone : availabilityZones) {
                        availabilityZoneVsRegion.put(availabilityZone, remoteRegion);
                    }
                }
            }

            logger.info("Availability zone to region mapping for all remote regions: {}", availabilityZoneVsRegion);
        } else {
            logger.info("Regions to fetch is null. Erasing older mapping if any.");
            availabilityZoneVsRegion.clear();
        }
    }

    protected abstract Set<String> getZonesForARegion(String region);

    @Override
    public String getRegionForAvailabilityZone(String availabilityZone) {
        return availabilityZoneVsRegion.get(availabilityZone);
    }

    private void populateDefaultAZToRegionMap() {
        defaultRegionVsAzMap.put("us-east-1", "us-east-1a");
        defaultRegionVsAzMap.put("us-east-1", "us-east-1c");
        defaultRegionVsAzMap.put("us-east-1", "us-east-1d");
        defaultRegionVsAzMap.put("us-east-1", "us-east-1e");

        defaultRegionVsAzMap.put("us-west-1", "us-west-1a");
        defaultRegionVsAzMap.put("us-west-1", "us-west-1c");

        defaultRegionVsAzMap.put("us-west-2", "us-west-2a");
        defaultRegionVsAzMap.put("us-west-2", "us-west-2b");
        defaultRegionVsAzMap.put("us-west-2", "us-west-2c");

        defaultRegionVsAzMap.put("eu-west-1", "eu-west-1a");
        defaultRegionVsAzMap.put("eu-west-1", "eu-west-1b");
        defaultRegionVsAzMap.put("eu-west-1", "eu-west-1c");
    }
}
