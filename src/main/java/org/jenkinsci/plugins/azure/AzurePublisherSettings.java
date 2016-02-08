/*
 * The MIT License
 *
 *  Copyright (c) 2015, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package org.jenkinsci.plugins.azure;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.FilePath;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.annotation.Nonnull;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.Override;import java.lang.String;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class AzurePublisherSettings extends BaseStandardCredentials implements FileCredentials {

    public static final String SUBSCRIPTION_XPATH = "/PublishData/PublishProfile/Subscription";

    private final String subscriptionId;

    private final String subscriptionName;

    private final String serviceManagementCert;

    private final String serviceManagementUrl;

/*
    public AzurePublisherSettings(CredentialsScope scope, String id, String description, FileItem file, String fileName, String data) throws IOException {
        super(scope, id, description);

        XPath xp = XPathFactory.newInstance().newXPath();
        InputSource ip = new InputSource(new ByteArrayInputStream(file.get()));
        try {
            Element node = (Element) xp.evaluate(SUBSCRIPTION_XPATH, ip, XPathConstants.NODE);
            this.subscriptionId = node.getAttribute("Id");
            this.subscriptionName = node.getAttribute("Name");
            this.serviceManagementCert = node.getAttribute("ManagementCertificate");
            this.serviceManagementUrl = node.getAttribute("ServiceManagementUrl");
        } catch (XPathExpressionException e) {
            throw new IOException("Invalid PublishSettings file", e);
        }
    }*/

    @DataBoundConstructor
    public AzurePublisherSettings(CredentialsScope scope, String id, String description, String subscriptionId, String subscriptionName, String serviceManagementCert, String serviceManagementUrl) {
        super(scope, id, description);
        this.subscriptionId = subscriptionId;
        this.subscriptionName = subscriptionName;
        this.serviceManagementCert = serviceManagementCert;
        this.serviceManagementUrl = serviceManagementUrl;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public String getServiceManagementCert() {
        return serviceManagementCert;
    }

    public String getServiceManagementUrl() {
        return serviceManagementUrl;
    }

    private String getPublisherSettings() {
        return
            "<?xml version='1.0' encoding='utf-8'?>\n" +
            "<PublishData>\n" +
            "  <PublishProfile\n" +
            "    SchemaVersion='2.0'\n" +
            "    PublishMethod='AzureServiceManagementAPI'>\n" +
            "    <Subscription\n" +
            "      ServiceManagementUrl='" + serviceManagementUrl + "'\n" +
            "      Id='" + subscriptionId + "'\n" +
            "      Name='" + subscriptionName + "'\n" +
            "      ManagementCertificate='" + serviceManagementCert + "'/>\n" +
            "  </PublishProfile>\n" +
            "</PublishData>\n";
    }

    /**
     * @return the content of the PublisherSettings file
     */
    @Nonnull
    public InputStream getPublisherSettingsFileContent() {
        String content = getPublisherSettings();
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Rebuild a valid Publishersettings (temporary) file for use with Azure CLI or comparable tooling
     */
    public FilePath getPublisherSettings(VirtualChannel channel) throws InterruptedException, IOException {
        return channel.call(new MasterToSlaveCallable   <FilePath, IOException>() {

            @Override
            public FilePath call() throws IOException {
                File f = File.createTempFile(subscriptionId, ".publishersettings");
                f.deleteOnExit();
                FileUtils.write(f, getPublisherSettings());
                return new FilePath(f);
            }
        });
    }

    @Nonnull
    @Override
    public String getFileName() {
        return getId() + ".publishsettings";
    }

    @Nonnull
    @Override
    public InputStream getContent() throws IOException {
        return getPublisherSettingsFileContent();
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        @Override public String getDisplayName() {
            return "Azure Publisher Settings";
        }

    }
}
