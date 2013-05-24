web: target/start -Dhttp.port=${PORT} ${JAVA_OPTS} -Dlogger.resource=${ENV_LOGGER}
worker: java ${JAVA_OPTS} -cp "target/staged/*" scheduler.RabbitMQ