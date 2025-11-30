package ca.pitt.demo;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonProperty;

@RegisterForReflection
public class IpamRecord {

    @JsonProperty("IPAddress")
    public String ipAddress;

    @JsonProperty("HostName")
    public String hostName;

    @JsonProperty("Status")
    public String status;

    @JsonProperty("MACAddress")
    public String macAddress;

    @JsonProperty("SubnetCIDR")
    public String subnetCidr;

    @JsonProperty("VLANID")
    public String vlanId;

    @JsonProperty("Gateway")
    public String gateway;

    @JsonProperty("OwnerTeam")
    public String ownerTeam;

    @JsonProperty("ExpirationDate")
    public String expirationDate;

    // Source Metadata
    @JsonProperty("SourceSystem")
    public String sourceSystem;

    @JsonProperty("OriginalRecord")
    public String originalRecord;

    public IpamRecord() {
    }

    @Override
    public String toString() {
        return "IpamRecord{" +
                "ipAddress='" + ipAddress + '\'' +
                ", hostName='" + hostName + '\'' +
                ", status='" + status + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", subnetCidr='" + subnetCidr + '\'' +
                ", vlanId='" + vlanId + '\'' +
                ", gateway='" + gateway + '\'' +
                ", ownerTeam='" + ownerTeam + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                ", sourceSystem='" + sourceSystem + '\'' +
                '}';
    }
}