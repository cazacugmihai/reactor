/*
 * Copyright (c) 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.dynamic;

import org.junit.Test;
import reactor.fn.Consumer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jon Brisbin
 */
public class DynamicReactorFactoryTests {

	@Test
	public void testCreatesDynamicReactors() throws InterruptedException {
		MyReactor myReactor = new DynamicReactorFactory<MyReactor>(MyReactor.class).create();

		final CountDownLatch latch = new CountDownLatch(2);
		myReactor.
								 onTest(new Consumer<String>() {
									 @Override
									 public void accept(String s) {
										 latch.countDown();
									 }
								 }).
								 onTestTest(new Consumer<String>() {
									 @Override
									 public void accept(String s) {
										 latch.countDown();
									 }
								 }).
								 notifyTest("Hello World!").
								 notifyTestTest("Hello World!");

		latch.await(5, TimeUnit.SECONDS);

		assertThat("Latch has been counted down", latch.getCount() == 0);
	}

}