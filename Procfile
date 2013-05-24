web: target/start -Dhttp.port=${PORT} ${JAVA_OPTS}
worker: java ${JAVA_OPTS} -cp "target/staged/*" scheduler.RabbitMQ