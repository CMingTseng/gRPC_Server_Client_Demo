package io.grpc.examples.helloworld;


import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import demo.grpc.proto.manualflowcontrol.getDemoAPIStreamingServiceGrpc;
import demo.grpc.transmission.composition.Hellorequest;
import demo.grpc.transmission.composition.Helloresponse;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;


/**
 * A simple GRPC server implementation which understands the protocol defined in hello.proto.
 *
 * Inspired by
 * <a href="https://github.com/grpc/grpc-java/blob/master/interop-testing/src/main/java/io/grpc/testing/integration/TestServiceServer.java">grpc-java's TestServiceServer</a>,
 * <a href="https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/manualflowcontrol/ManualFlowControlServer.java">grpc-java's TestServiceServer</a>,
 * but radically simplified.
 *
 * When changing the proto file you also have to adapt the {@link #start()} method. For more complex
 * implementations extracting the service implementation in an own class is advised.
 */
public class HelloWorldBasicStreamingServer {
    private static final Logger logger = Logger.getLogger(HelloWorldBasicStreamingServer.class.getName());

    public static void main(String[] args) throws InterruptedException, IOException {
        // Service class implementation

        final Server server = NettyServerBuilder
                .forPort(8080)
                .addService(new GreeterImpl())
                .build()
                .start();

        logger.info("Listening on " + server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutting down");
                server.shutdown();
            }
        });
        server.awaitTermination();
    }

    static class GreeterImpl extends getDemoAPIStreamingServiceGrpc.getDemoAPIStreamingServiceImplBase {
        @Override
        public StreamObserver<Hellorequest.HelloRequest> sayHelloAPIStreaming(final StreamObserver<Helloresponse.HelloResponse> responseObserver) {

            // Set up manual flow control for the request stream. It feels backwards to configure the request
            // stream's flow control using the response stream's observer, but this is the way it is.
            final ServerCallStreamObserver<Helloresponse.HelloResponse> serverCallStreamObserver =
                    (ServerCallStreamObserver<Helloresponse.HelloResponse>) responseObserver;
            serverCallStreamObserver.disableAutoInboundFlowControl();

            // Guard against spurious onReady() calls caused by a race between onNext() and onReady(). If the transport
            // toggles isReady() from false to true while onNext() is executing, but before onNext() checks isReady(),
            // request(1) would be called twice - once by onNext() and once by the onReady() scheduled during onNext()'s
            // execution.
            final AtomicBoolean wasReady = new AtomicBoolean(false);

            // Set up a back-pressure-aware consumer for the request stream. The onReadyHandler will be invoked
            // when the consuming side has enough buffer space to receive more messages.
            //
            // Note: the onReadyHandler's invocation is serialized on the same thread pool as the incoming StreamObserver's
            // onNext(), onError(), and onComplete() handlers. Blocking the onReadyHandler will prevent additional messages
            // from being processed by the incoming StreamObserver. The onReadyHandler must return in a timely manor or else
            // message processing throughput will suffer.
            serverCallStreamObserver.setOnReadyHandler(new Runnable() {
                public void run() {
                    if (serverCallStreamObserver.isReady() && wasReady.compareAndSet(false, true)) {
                        logger.info("READY");
                        // Signal the request sender to send one message. This happens when isReady() turns true, signaling that
                        // the receive buffer has enough free space to receive more messages. Calling request() serves to prime
                        // the message pump.
                        serverCallStreamObserver.request(1);
                    }
                }
            });

            // Give gRPC a StreamObserver that can observe and process incoming requests.
            return new StreamObserver<Hellorequest.HelloRequest>() {
                @Override
                public void onNext(Hellorequest.HelloRequest request) {
                    // Process the request and send a response or an error.
                    try {
                        // Accept and enqueue the request.
                        String name = request.getName();
                        logger.info("--> " + name);

                        // Simulate server "work"
                        Thread.sleep(100);

                        // Send a response.
                        String message = "Hello " + name;
                        logger.info("<-- " + message);
                        Helloresponse.HelloResponse reply = Helloresponse.HelloResponse.newBuilder().setMessage(message).build();
                        responseObserver.onNext(reply);

                        // Check the provided ServerCallStreamObserver to see if it is still ready to accept more messages.
                        if (serverCallStreamObserver.isReady()) {
                            // Signal the sender to send another request. As long as isReady() stays true, the server will keep
                            // cycling through the loop of onNext() -> request()...onNext() -> request()... until either the client
                            // runs out of messages and ends the loop or the server runs out of receive buffer space.
                            //
                            // If the server runs out of buffer space, isReady() will turn false. When the receive buffer has
                            // sufficiently drained, isReady() will turn true, and the serverCallStreamObserver's onReadyHandler
                            // will be called to restart the message pump.
                            serverCallStreamObserver.request(1);
                        } else {
                            // If not, note that back-pressure has begun.
                            wasReady.set(false);
                        }
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                        responseObserver.onError(
                                Status.UNKNOWN.withDescription("Error handling request").withCause(throwable).asException());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    // End the response stream if the client presents an error.
                    t.printStackTrace();
                    responseObserver.onCompleted();
                }

                @Override
                public void onCompleted() {
                    // Signal the end of work when the client ends the request stream.
                    logger.info("COMPLETED");
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
