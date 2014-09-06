/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.maven.plugin;

import com.github.thorqin.webapi.Publisher;
import java.io.IOException;
import java.text.ParseException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author nuo.qin
 * @
 */
@Mojo(name="ssi-compile", 
		defaultPhase=LifecyclePhase.PREPARE_PACKAGE)
public class PageCompiler extends AbstractMojo {

	@Parameter(defaultValue="${basedir}/src/main/ssi")
	String ssiDirectory;
	
	@Parameter(defaultValue="${project.build.directory}/${project.build.finalName}")
	String targetDirectory;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Generate SSI pages...");
		try {
			final int[] generatedCount = new int[1];
			generatedCount[0] = 0;
			Publisher publisher = new Publisher(ssiDirectory, targetDirectory);
			publisher.setPublishEventListener(new Publisher.PublishEventListener() {
				@Override
				public void fileCreated(String path) {
					getLog().info("Created: " + path);
					generatedCount[0]++;
				}
			});
			publisher.publish();
			getLog().info("Generated " + generatedCount[0] + " files.");
		} catch (IOException | ParseException ex) {
			throw new MojoFailureException("Compile SSI pages failed: " + ex.getMessage(), ex);
		}
	}
}
