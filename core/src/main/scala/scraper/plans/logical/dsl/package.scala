package scraper.plans.logical

import scraper.Name
import scraper.expressions._
import scraper.expressions.AutoAlias.named
import scraper.expressions.functions._

package object dsl {
  trait LogicalPlanDSL {
    def plan: LogicalPlan

    def select(projectList: Seq[Expression]): Project =
      Project(plan, projectList map {
        case UnresolvedAttribute(name, qualifier) if name.casePreserving == "*" => Star(qualifier)
        case e => named(e)
      })

    def select(first: Expression, rest: Expression*): Project = select(first +: rest)

    def filter(condition: Expression): Filter = Filter(plan, condition)

    def filterOption(predicates: Seq[Expression]): LogicalPlan =
      predicates reduceOption And map filter getOrElse plan

    def where(condition: Expression): Filter = filter(condition)

    def having(condition: Expression): Filter = filter(condition)

    def limit(n: Expression): Limit = Limit(plan, n)

    def limit(n: Int): Limit = this limit lit(n)

    def orderBy(order: Seq[SortOrder]): Sort = Sort(plan, order)

    def orderBy(first: SortOrder, rest: SortOrder*): Sort = this orderBy (first +: rest)

    def orderByOption(order: Seq[SortOrder]): LogicalPlan =
      if (order.nonEmpty) orderBy(order) else plan

    def distinct: Distinct = Distinct(plan)

    def subquery(name: Name): Subquery = Subquery(plan, name)

    def join(that: LogicalPlan): Join = Join(plan, that, Inner, None)

    def leftSemiJoin(that: LogicalPlan): Join = Join(plan, that, LeftSemi, None)

    def leftJoin(that: LogicalPlan): Join = Join(plan, that, LeftOuter, None)

    def rightJoin(that: LogicalPlan): Join = Join(plan, that, RightOuter, None)

    def outerJoin(that: LogicalPlan): Join = Join(plan, that, FullOuter, None)

    def union(that: LogicalPlan): Union = Union(plan, that)

    def intersect(that: LogicalPlan): Intersect = Intersect(plan, that)

    def except(that: LogicalPlan): Except = Except(plan, that)

    def groupBy(keys: Seq[Expression]): UnresolvedAggregateBuilder =
      new UnresolvedAggregateBuilder(plan, keys)

    def groupBy(first: Expression, rest: Expression*): UnresolvedAggregateBuilder =
      new UnresolvedAggregateBuilder(plan, first +: rest)

    def agg(projectList: Seq[Expression]): UnresolvedAggregate = this groupBy Nil agg projectList

    def agg(first: Expression, rest: Expression*): UnresolvedAggregate = agg(first +: rest)

    def resolvedGroupBy(keys: Seq[GroupingAlias]): ResolvedAggregateBuilder =
      new ResolvedAggregateBuilder(plan, keys)

    def resolvedGroupBy(first: GroupingAlias, rest: GroupingAlias*): ResolvedAggregateBuilder =
      resolvedGroupBy(first +: rest)

    def resolvedAgg(functions: Seq[AggregationAlias]): Aggregate =
      plan resolvedGroupBy Nil agg functions

    def resolvedAgg(first: AggregationAlias, rest: AggregationAlias*): Aggregate =
      resolvedAgg(first +: rest)
  }

  class UnresolvedAggregateBuilder(plan: LogicalPlan, keys: Seq[Expression]) {
    def agg(projectList: Seq[Expression]): UnresolvedAggregate =
      UnresolvedAggregate(plan, keys, projectList map named)

    def agg(first: Expression, rest: Expression*): UnresolvedAggregate = agg(first +: rest)
  }

  class ResolvedAggregateBuilder(plan: LogicalPlan, keys: Seq[GroupingAlias]) {
    def agg(functions: Seq[AggregationAlias]): Aggregate = Aggregate(plan, keys, functions)

    def agg(first: AggregationAlias, rest: AggregationAlias*): Aggregate = agg(first +: rest)
  }

  def table(name: Name): UnresolvedRelation = UnresolvedRelation(name)

  def values(expressions: Seq[Expression]): Project = SingleRowRelation select expressions

  def values(first: Expression, rest: Expression*): Project = values(first +: rest)

  def let(cteRelation: (Symbol, LogicalPlan))(body: LogicalPlan): With = {
    val (name, value) = cteRelation
    With(body, name, value)
  }
}
