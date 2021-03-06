/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
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
package reactor.groovy

import groovy.transform.CompileStatic
import reactor.core.Reactor

import static reactor.event.selector.Selectors.$

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import reactor.core.Environment
import reactor.core.spec.Reactors
import reactor.event.Event
import reactor.event.dispatch.EventLoopDispatcher
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Stephane Maldini (smaldini)
 */
class GroovyReactorSpec extends Specification {

	@Shared def testEnv

	void setupSpec(){
		testEnv = new Environment()
		testEnv.addDispatcher('eventLoop',new EventLoopDispatcher('eventLoop', 256))
	}

	def "Groovy Reactor dispatches events properly"() {

		given: "a simple reactor implementation"
		def r1 = Reactors.reactor().get()
		def r2 = Reactors.reactor().get()
		def latch = new CountDownLatch(1)

		when: 'Using simple arguments'
		def result = ""
		r1.on('test2') { String s ->
			result = s
			latch.countDown()
		}
		r1.notify 'test2', 'Hello'

		then:
		latch.await(5, TimeUnit.SECONDS)
		result == 'Hello'

		when: 'Using Selector and Consumer<Event> arguments'
		def data = ""
		def header = ""
		latch = new CountDownLatch(1)

		r2.on($('test')) { Event<String> s ->
			data = s.data
			header = s.headers['someHeader']
			latch.countDown()
		}
		r2.notify for: 'test', data: 'Hello World!', someHeader: 'test'

		then:
		latch.await(5, TimeUnit.SECONDS)
		data == "Hello World!"
		header == "test"

	}

	def "Groovy Reactor provides Closure as Supplier on notify"() {

		given: "a simple Reactor"
		def r = Reactors.reactor().get()
		def result = ""
		r.on('supplier') { String s ->
			result = s
		}

		when: "a supplier is provided"
		r.notify('supplier', { "Hello World!" })

		then: "the result has been set"
		result == "Hello World!"

	}

	def "Groovy Reactor allows inline reply"() {

		given: "a simple reactor implementation"
		def reactor = Reactors.reactor().get()

		when: 'Using simple arguments'
		def data2 = ""
		reactor.on($('test')){ String s ->
			reply(s + ' ok')
		}  // ugly hack until I can get Groovy Closure invocation support built-in

		reactor.send('test', 'send'){
			data2 = 'test3'
		}

		then:
		data2 == 'test3'

	}

	def "Compile Static Reactor"(){
		given:
			final reactor.core.Environment env = new reactor.core.Environment()

			final Reactor reactor = Reactors.reactor()
					.env(env) // our current Environment
					.dispatcher(Environment.THREAD_POOL)
					.get()

		when:
			"A simple scenario"

			def consumer = new Consumer(r:reactor)
			consumer.setupMessages()
			def producer = new Producer(r:reactor)
			producer.makeNoise('Yeah we is awesome')
			consumer.result.await()

		then:
			consumer.result.count == 0
	}

	//FIXME Groovy issue -> invokes Reactor.notify(Object key) instead of Observable.extensions(Observable self,
	// Map params)
	//@CompileStatic
	class Producer{
		Reactor r
		void makeNoise(String noise){
			r.notify for: 'makeNoise', data: noise
		}
	}

	class Consumer{
		Reactor r
		def result = new CountDownLatch(1)

		void setupMessages(){
			r.on($('makeNoise')) { String noise ->
				println noise
				result.countDown()
			}
		}
	}

}
