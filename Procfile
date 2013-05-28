web: target/start -Dhttp.port=${PORT} ${JAVA_OPTS} -Dlogger.file=${ENV_LOGGER}
worker: java ${JAVA_OPTS} -cp "target/staged/*" scheduler.RabbitMQ