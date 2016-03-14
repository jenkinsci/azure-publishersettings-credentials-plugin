/*
 * The MIT License
 *
 *  Copyright (c) 2016, CloudBees, Inc.
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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.credentialsbinding.Binding;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.UUID;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class AzurePublisherSettingsBinding extends Binding<AzurePublisherSettings> {

    @DataBoundConstructor
    public AzurePublisherSettingsBinding(String variable, String credentialsId) {
        super(variable, credentialsId);
    }

    @Override
    protected Class<AzurePublisherSettings> type() {
        return AzurePublisherSettings.class;
    }


    // Mostly a copy paste from org.jenkinsci.plugins.credentialsbinding.impl.FileBinding

    @Override public SingleEnvironment bindSingle(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        AzurePublisherSettings credentials = getCredentials(build);
        FilePath secrets = secretsDir(workspace);
        String dirName = UUID.randomUUID().toString();
        final FilePath dir = secrets.child(dirName);
        dir.mkdirs();
        secrets.chmod(/*0700*/448);
        FilePath secret = dir.child(credentials.getId()+".publishsettings");
        secret.copyFrom(credentials.getPublisherSettingsFileContent());
        secret.chmod(0400);
        return new SingleEnvironment(secret.getRemote(), new UnbinderImpl(dirName));
    }

    private static class UnbinderImpl implements Unbinder {

        private static final long serialVersionUID = 1;

        private final String dirName;

        UnbinderImpl(String dirName) {
            this.dirName = dirName;
        }

        @Override public void unbind(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            secretsDir(workspace).child(dirName).deleteRecursive();
        }

    }

    private static FilePath secretsDir(FilePath workspace) {
        Computer computer = workspace.toComputer();
        Node node = computer == null ? null : computer.getNode();
        FilePath root = node == null ? workspace : node.getRootPath();
        return root.child("secretFiles");
    }

    @Extension
    public static class DescriptorImpl extends BindingDescriptor<AzurePublisherSettings> {

        @Override protected Class<AzurePublisherSettings> type() {
            return AzurePublisherSettings.class;
        }

        @Override public String getDisplayName() {
            return "Azure Publish Settings";
        }

    }
}
