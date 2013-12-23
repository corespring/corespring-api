web: target/universal/stage/bin/corespring -Dhttp.port=${PORT} ${JAVA_OPTS} -Dlogger.file=${ENV_LOGGER}
worker: java ${JAVA_OPTS} -cp "target/universal/stage/lib/*" scheduler.RabbitMQ