package com.google.jenkins.plugins.computeengine.client;

import com.google.api.services.compute.model.*;
import com.google.jenkins.plugins.computeengine.AcceleratorConfiguration;
import com.google.jenkins.plugins.computeengine.ComputeEngineCloud;
import com.google.jenkins.plugins.computeengine.InstanceConfiguration;
import hudson.model.Node;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.imageio.ImageTypeSpecifier;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class InstanceConfigurationTest {
    static final String NAME_PREFIX = "test";
    static final String PROJECT_ID = "test-project";
    static final String REGION = "us-west1";
    static final String ZONE = "us-west1-a";
    static final String LABEL = "LABEL1, LABEL2";
    static final String MACHINE_TYPE = "n1-standard-1";
    static final String STARTUP_SCRIPT = "#!/bin/bash";
    static final boolean PREEMPTIBLE = true;
    static final String CONFIG_DESC = "test-config";
    static final String BOOT_DISK_TYPE = "pd-standard";
    static final boolean BOOT_DISK_AUTODELETE = true;
    static final String BOOT_DISK_IMAGE_NAME = "test-image";
    static final String BOOT_DISK_PROJECT_ID = PROJECT_ID;
    static final String BOOT_DISK_SIZE_GB_STR = "10";
    static final Node.Mode NODE_MODE = Node.Mode.EXCLUSIVE;
    static final String ACCELERATOR_NAME = "test-gpu";
    static final String ACCELERATOR_COUNT = "1";
    public final String NETWORK_NAME = "test-network";
    public final String SUBNETWORK_NAME = "test-subnetwork";
    public final boolean EXTERNAL_ADDR = true;
    public final String NETWORK_TAGS = "tag1 tag2";
    public final String SERVICE_ACCOUNT_EMAIL = "test-service-account";


    @Mock
    public ComputeClient computeClient;

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Before
    public void init() throws Exception {
        List<Region> regions = new ArrayList<Region>();
        regions.add(new Region().setName("").setSelfLink(""));
        regions.add(new Region().setName(REGION).setSelfLink(REGION));

        List<Zone> zones = new ArrayList<Zone>();
        zones.add(new Zone().setName("").setSelfLink(""));
        zones.add(new Zone().setName(ZONE).setSelfLink(ZONE));

        List<MachineType> machineTypes = new ArrayList<MachineType>();
        machineTypes.add(new MachineType().setName("").setSelfLink(""));
        machineTypes.add(new MachineType().setName(MACHINE_TYPE).setSelfLink(MACHINE_TYPE));

        List<DiskType> diskTypes = new ArrayList<DiskType>();
        diskTypes.add(new DiskType().setName("").setSelfLink(""));
        diskTypes.add(new DiskType().setName(BOOT_DISK_TYPE).setSelfLink(BOOT_DISK_TYPE));

        List<Image> imageTypes = new ArrayList<Image>();
        imageTypes.add(new Image().setName("").setSelfLink(""));
        imageTypes.add(new Image().setName(BOOT_DISK_IMAGE_NAME).setSelfLink(BOOT_DISK_IMAGE_NAME));

        List<Network> networks = new ArrayList<Network>();
        networks.add(new Network().setName("").setSelfLink(""));
        networks.add(new Network().setName(NETWORK_NAME).setSelfLink(NETWORK_NAME));

        List<Subnetwork> subnetworks = new ArrayList<Subnetwork>();
        subnetworks.add(new Subnetwork().setName("").setSelfLink(""));
        subnetworks.add(new Subnetwork().setName(SUBNETWORK_NAME).setSelfLink(SUBNETWORK_NAME));

        List<AcceleratorType> acceleratorTypes = new ArrayList<AcceleratorType>();
        acceleratorTypes.add(new AcceleratorType().setName("").setSelfLink("").setMaximumCardsPerInstance(0));
        acceleratorTypes.add(new AcceleratorType().setName(ACCELERATOR_NAME).setSelfLink(ACCELERATOR_NAME).setMaximumCardsPerInstance(Integer.parseInt(ACCELERATOR_COUNT)));

        Mockito.when(computeClient.getRegions()).thenReturn(regions);
        Mockito.when(computeClient.getZones(anyString())).thenReturn(zones);
        Mockito.when(computeClient.getMachineTypes(anyString())).thenReturn(machineTypes);
        Mockito.when(computeClient.getBootDiskTypes(anyString())).thenReturn(diskTypes);
        Mockito.when(computeClient.getImages(anyString())).thenReturn(imageTypes);
        Mockito.when(computeClient.getAcceleratorTypes(anyString())).thenReturn(acceleratorTypes);
        Mockito.when(computeClient.getNetworks(anyString())).thenReturn(networks);
        Mockito.when(computeClient.getSubnetworks(anyString(), anyString(), anyString())).thenReturn(subnetworks);

        computeClient.setProjectId(PROJECT_ID);
    }

    @Test
    public void testClient() throws Exception {
        List<Region> regions = computeClient.getRegions();
        assert (regions.size() == 2);
        assert (regions.get(1).getName().equals(REGION));

        List<Zone> zones = computeClient.getZones(REGION);
        assert (zones.size() == 2);
        assert (zones.get(1).getName().equals(ZONE));
        assert (zones.get(1).getSelfLink().equals(ZONE));
    }

    @Test
    public void testConfigRoundtrip() throws Exception {

        InstanceConfiguration want = instanceConfiguration();

        InstanceConfiguration.DescriptorImpl.setComputeClient(computeClient);
        AcceleratorConfiguration.DescriptorImpl.setComputeClient(computeClient);

        List<InstanceConfiguration> configs = new ArrayList<InstanceConfiguration>();
        configs.add(want);

        ComputeEngineCloud gcp = new ComputeEngineCloud("test", PROJECT_ID, "testCredentialsId", "1", configs);
        r.jenkins.clouds.add(gcp);

        r.submit(r.createWebClient().goTo("configure").getFormByName("config"));
        InstanceConfiguration got = ((ComputeEngineCloud) r.jenkins.clouds.iterator().next()).getInstanceConfig(CONFIG_DESC);
        r.assertEqualBeans(want, got, "namePrefix,region,zone,machineType,preemptible,startupScript,bootDiskType,bootDiskSourceImageName,bootDiskSourceImageProject,bootDiskSizeGb,acceleratorConfiguration,network,subnetwork,externalAddress,networkTags,serviceAccountEmail");
    }

    @Test
    public void testInstanceModel() {
        Instance i = instanceConfiguration().instance();
        assert (i.getZone().equals(ZONE));
        assert (i.getMachineType().equals(MACHINE_TYPE));
        assert (i.getMetadata().getItems().get(0).getKey().equals(InstanceConfiguration.METADATA_STARTUP_SCRIPT_KEY));
        assert (i.getMetadata().getItems().get(0).getValue().equals(STARTUP_SCRIPT));
        assert (i.getServiceAccounts().get(0).getEmail().equals(SERVICE_ACCOUNT_EMAIL));
    }

    private InstanceConfiguration instanceConfiguration() {
        return new InstanceConfiguration(
                NAME_PREFIX,
                REGION,
                ZONE,
                MACHINE_TYPE,
                STARTUP_SCRIPT,
                PREEMPTIBLE,
                LABEL,
                CONFIG_DESC,
                BOOT_DISK_TYPE,
                BOOT_DISK_AUTODELETE,
                BOOT_DISK_IMAGE_NAME,
                BOOT_DISK_PROJECT_ID,
                BOOT_DISK_SIZE_GB_STR,
                NETWORK_NAME,
                SUBNETWORK_NAME,
                EXTERNAL_ADDR,
                NETWORK_TAGS,
                SERVICE_ACCOUNT_EMAIL,
                NODE_MODE,
                new AcceleratorConfiguration(ACCELERATOR_NAME, ACCELERATOR_COUNT));
    }

}
