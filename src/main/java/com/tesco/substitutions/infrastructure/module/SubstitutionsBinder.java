package com.tesco.substitutions.infrastructure.module;

//
//public class SubstitutionsBinder extends AbstractModule {
//
//    private final Vertx vertx;
//    private final Context context;
//
//    public SubstitutionsBinder(final Vertx vertx) {
//        this.vertx = vertx;
//        this.context = vertx.getOrCreateContext();
//    }
//
//    @Override
//    protected void configure() {
//        this.bind(Vertx.class).toInstance(this.vertx);
//        this.bind(SubstitutionsService.class).to(SubstitutesRedisService.class);
//        this.bindRoutes();
//    }
//
//    @Provides
//    @Singleton
//    @Named("substitutionsEndpoints")
//    static List<EndpointDefinition> endpointDefinitions(final StatusEndpointDefinition statusEndpointDefinition,
//            final SubsEndpointDefinition subsEndpointDefinition) {
//        return ImmutableList.of(statusEndpointDefinition, subsEndpointDefinition);
//    }
//
//    // Empty as RouterFactory expects the eventBusDefinitions as well
//    @Provides
//    @Singleton
//    static Set<EventBusDefinition> eventBusDefinitions() {
//        return new HashSet<>();
//    }
//
//    private void bindRoutes() {
//        final Multibinder<RoutesDefinition> routesBinder = Multibinder.newSetBinder(this.binder(), RoutesDefinition.class);
//        routesBinder.addBinding().to(SubstitutionsRoutes.class);
//
//    }
//
////    @Provides
////    @Singleton
////    @Named("redisClient")
////    public RedisClient redisClient() {
////        final JsonObject redisConfiguration = this.context.config().getJsonObject("redisConfiguration");
////        final RedisOptions redisOptions = new RedisOptions(redisConfiguration);
////        return RedisClient.create(this.vertx, redisOptions);
////    }
//}
